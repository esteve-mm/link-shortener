using System.Text;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization;
using InfluxDB.Client;
using InfluxDB.Client.Api.Domain;
using Nest;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;
using JsonSerializer = System.Text.Json.JsonSerializer;

namespace metrics_service
{
    public class Worker : BackgroundService
    {
        private readonly ILogger<Worker> _logger;
        private readonly IConfiguration _settings;
        private InfluxDBClient _influxClient;
        private IElasticClient _elasticClient;

        private IConnection _connection;
        private IModel _channel;
    
        public Worker(
            ILogger<Worker> logger, 
            IConfiguration settings)
        {
            _logger = logger;
            _settings = settings;
        }

        public override async Task StartAsync(CancellationToken cancellationToken)
        {
            ConnectToRabbit();
            ConnectToDb();
            await base.StartAsync(cancellationToken);
        }

        protected override async Task ExecuteAsync(CancellationToken cancellationToken)
        {
            var consumer = new EventingBasicConsumer(_channel);
            consumer.Received += (_, message) =>
            {
                var entity = message.Exchange;
                var action = message.RoutingKey;
                var content = Encoding.Default.GetString(message.Body.ToArray());
                try
                {
                    _logger.LogInformation("Event receivied {Event}: {Content}", action, content);

                    // InfluxDB
                    if (action == "link.redirected")
                    {
                        var linkRedirected = JsonSerializer.Deserialize<LinkRedirectedEvent>(content);
                        if (linkRedirected != null)
                        {
                            using var write = _influxClient.GetWriteApi();
                            var record = InfluxDB.Client.Writes.PointData.Measurement("link-redirects")
                                .Tag("link", linkRedirected.Id.ToString())
                                .Tag("original", linkRedirected.Original)
                                .Tag("shortened", linkRedirected.Shortened)
                                .Field("latency", linkRedirected.Latency)
                                .Timestamp(linkRedirected.Timestamp.ToUniversalTime(), WritePrecision.Ns);
                            write.WritePoint(record, "reporting", "shrtr");
                        }
                    }
                    // elastic
                    else
                    {
                        var ev = new Event
                        {
                            Entity = entity,
                            Type = action,
                            ReceivedAt = DateTime.Now,
                            Data = Deserialize(content)
                        };
                        
                        var response = _elasticClient.IndexDocument(ev);
                        if (!response.IsValid)
                        {
                            throw new Exception(response.ServerError?.Error?.Reason);
                        }
                    }

                    _channel.BasicAck(message.DeliveryTag, false);
                }
                catch (Exception e)
                {
                    // TODO: Ack or Nack depending on error
                    _logger.LogError(e, "Oops :/ somethind went wrong,");
                    _channel.BasicNack(message.DeliveryTag, false,false );
                }
            };
            
            var queues = GetQueues();
            foreach (var queue in queues)
            {
                _channel.BasicConsume(queue.QueueName, false, consumer);
            }

            while (!cancellationToken.IsCancellationRequested)
            {
                _logger.LogInformation("Worker running at: {Time}", DateTimeOffset.Now);
                await Task.Delay(5000, cancellationToken);
            }
        }

        public override async Task StopAsync(CancellationToken cancellationToken)
        {
            await base.StopAsync(cancellationToken);
            _channel.Close();
            _connection.Close();
            _logger.LogInformation("RabbitMQ connection closed");
        }
        
        private void ConnectToRabbit()
        {
            string rabbitHost = _settings.GetValue<string>("RabbitMQ:Host");
            int rabbitPort = _settings.GetValue<int>("RabbitMQ:Port");
            _logger.LogInformation("Connecting to rabbit at {Host}:{Port}", rabbitHost, rabbitPort);
            
            var connectionFactory = new ConnectionFactory
            {
                HostName = rabbitHost,
                Port = rabbitPort,
                UserName = _settings.GetValue<string>("RabbitMQ:Username"),
                Password = _settings.GetValue<string>("RabbitMQ:Password")
            };
            _connection = connectionFactory.CreateConnection();
            _connection.ConnectionShutdown += HandleConnectionShutdown;
            _channel = _connection.CreateModel();
            
            // Assert exchanges exist
            var exchanges = GetExchanges();
            foreach (var exchange in exchanges)
            {
                _channel.ExchangeDeclarePassive(exchange);
            }
            
            // Declare queues
            var queues = GetQueues();
            foreach (var queue in queues)
            {
                _channel.QueueDeclare(queue.QueueName, true, false, false);
                _channel.QueueBind(queue.QueueName, queue.Exchange, queue.RoutingKey);
            }

            _logger.LogInformation("Connected to rabbit. Waiting for messages");
        }

        private void HandleConnectionShutdown(object sender, ShutdownEventArgs e)
        {
            Console.WriteLine("Connection to RabbitMq lost! Trying to reconnect...");

            const int maxRetries = 3;
            CleanUpRabbitConnection();

            for (int retry = 0; retry < maxRetries; retry++)
            {
                try
                {
                    ConnectToRabbit();
                }
                catch (RabbitMQ.Client.Exceptions.BrokerUnreachableException)
                {
                    Console.WriteLine("Reconnect failed!");
                    Thread.Sleep(3000);
                }
            }
            
            Console.WriteLine("Could not reconnect to RabbitMq. Max retries exceeded.");
        }
        
        private void CleanUpRabbitConnection()
        {
            try
            {
                if (_channel is { IsOpen: true })
                {
                    _channel.Close();
                    _channel = null;
                }

                if (_connection is { IsOpen: true })
                {
                    _connection.Close();
                    _connection = null;
                }
            }
            catch (IOException)
            {
                
            }
        }
        
        private void ConnectToDb()
        {
            _influxClient = InfluxDBClientFactory.Create(
                _settings.GetValue<string>("InfluxDB:ConnectionString"),
                _settings.GetValue<string>("InfluxDB:Token"));

            var settings = new ConnectionSettings(new Uri(_settings.GetValue<string>("Elasticsearch:URL")))
                .DefaultIndex("events"); 
            _elasticClient = new ElasticClient(settings);
        }
        
        private List<string> GetExchanges()
        {
            var events = _settings.GetValue<string>("MetricsService:ListenToEvents").Split(",");
            return events.Select(ev => ev.Split(".")[0])
                .Distinct()
                .ToList();
        }
        
        private List<Queue> GetQueues()
        {
            var events = _settings.GetValue<string>("MetricsService:ListenToEvents").Split(",");

            return events.Select(ev =>
            {
                string entity = ev.Split(".")[0];
                string action = ev.Split(".")[1];
                return new Queue
                {
                    Exchange = entity,
                    QueueName = $"{entity}-{action}-queue",
                    RoutingKey = ev
                };
            }).ToList();
        }

        private static object Deserialize(string json)
        {
            return ToObject(JToken.Parse(json));
        }

        private static object ToObject(JToken token)
        {
            switch (token.Type)
            {
                case JTokenType.Object:
                    return token.Children<JProperty>()
                        .ToDictionary(prop => prop.Name,
                            prop => ToObject(prop.Value));

                case JTokenType.Array:
                    return token.Select(ToObject).ToList();

                default:
                    return ((JValue)token).Value;
            }
        }

        public class Event
        {
            public string Entity { get; set; }
            public string Type { get; set; }
            public DateTime ReceivedAt { get; set; }
            public object Data { get; set; }
        }

        private readonly struct Queue
        {
            public string Exchange { get; init; }
            public string QueueName { get; init; }
            public string RoutingKey { get; init; }
        }

        private class LinkRedirectedEvent
        {
            [JsonPropertyName("timestamp")] public DateTime Timestamp { get; set; }
            [JsonPropertyName("id")] public Guid Id { get; set; }
            [JsonPropertyName("original")] public string Original { get; set; }
            [JsonPropertyName("shortened")] public string Shortened { get; set; }
            [JsonPropertyName("owner")] public string Owner { get; set; }
            [JsonPropertyName("latency")] public long Latency { get; set; }
        }

    }
}

