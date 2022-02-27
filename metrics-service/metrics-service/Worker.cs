using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using MongoDB.Bson;
using MongoDB.Bson.Serialization;
using MongoDB.Driver;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;

namespace metrics_service
{
    public class Worker : BackgroundService
    {
        private readonly ILogger<Worker> _logger;
        private readonly IConfiguration _settings;
        private IMongoDatabase _db;

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
                    
                    var events = _db.GetCollection<BsonDocument>("events");
                    events.InsertOne(new BsonDocument
                    {
                        ["entity"] = entity,
                        ["type"] = action,
                        ["receivedAt"] = DateTime.Now,
                        ["data"] =  BsonSerializer.Deserialize<BsonDocument>(content) 

                    }, cancellationToken: cancellationToken);
                    
                    _channel.BasicAck(message.DeliveryTag, false);
                }
                catch
                {
                    // TODO: Ack or Nack depending on error
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
            catch (IOException e)
            {
                
            }
        }
        
        private void ConnectToDb()
        {
            _db = new MongoClient(_settings.GetValue<string>("MongoDB:ConnectionString"))
                .GetDatabase("eventsdb");
        }

        private List<string> GetExchanges()
        {
            var exchanges = _settings.GetValue<string>("MetricsService:Entities")
                .Split(",")
                .Select(e => e.ToLower());
            return exchanges.ToList();
        }

        private List<Queue> GetQueues()
        {
            var entities = _settings.GetValue<string>("MetricsService:Entities").Split(",");
            var events = _settings.GetValue<string>("MetricsService:Events").Split(",");
            
            return (from entity in entities
                from ev in events
                select new Queue
                {
                    Entity = entity,
                    Exchange = entity.ToLower(),
                    Topic = $"{entity}.{ev}".ToLower(),
                    QueueName = $"{entity}-{ev}-queue".ToLower(),
                    RoutingKey = $"{entity}.{ev}".ToLower(),
                }).ToList();
        }

        private readonly struct Queue
        {
            public string Entity { get; init; }
            public string Exchange { get; init; }
            public string Topic { get; init; }
            public string QueueName { get; init; }
            public string RoutingKey { get; init; }
        }
    }
}

