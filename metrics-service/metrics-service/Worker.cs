using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;

namespace metrics_service
{
    public class Worker : BackgroundService
    {
        private readonly ILogger<Worker> _logger;
        private readonly IConfiguration _settings;
        
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
            await TryConnectToRabbit(connectionFactory, cancellationToken);
            _channel = _connection.CreateModel();

            var queues = GetQueues();
            foreach (var queue in queues)
            {
                _channel.QueueDeclarePassive(queue);//Queue must exist when using QueueDeclarePassive
            }
            
            _channel.BasicQos(0, 1, false);
            _logger.LogInformation($"Connected to rabbit. Waiting for messages.");
            
            await base.StartAsync(cancellationToken);
        }

        private async Task TryConnectToRabbit(ConnectionFactory connectionFactory, CancellationToken cancellationToken)
        {
            const int maxConnectionAttempts = 3;
            int currenAttempt = 1;
            while (true)
            {
                try
                {
                    _connection = connectionFactory.CreateConnection();
                    break;
                }
                catch (RabbitMQ.Client.Exceptions.BrokerUnreachableException e)
                {
                    if (currenAttempt++ > maxConnectionAttempts)
                    {
                        throw;
                    }
                    await Task.Delay(10000, cancellationToken);
                }
            }
        }
        
        private IEnumerable<string> GetQueues()
        {
            var entities = _settings.GetValue<string>("MetricsService:Entities").Split(",");
            var events = _settings.GetValue<string>("MetricsService:Events").Split(",");

            return (
                from entity in entities 
                from even in events 
                select $"{entity}-{even}-queue".ToLower())
                .ToList();
        }

        protected override async Task ExecuteAsync(CancellationToken cancellationToken)
        {
            var consumer = new EventingBasicConsumer(_channel);
            consumer.Received += (sender, message) =>
            {
                var entity = message.Exchange;
                var action = message.RoutingKey;
                var content = Encoding.Default.GetString(message.Body.ToArray());
                try
                {
                    _logger.LogInformation("Event receivied {Event}: {Content}", action, content);
                    
                    // Do stuff
                    
                    _channel.BasicAck(message.DeliveryTag, false);
                }
                catch
                {
                    _channel.BasicNack(message.DeliveryTag, false,false );
                }
            };
            
            var queues = GetQueues();
            foreach (var queue in queues)
            {
                _channel.BasicConsume(queue, false, consumer);
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
            _connection.Close();
            _logger.LogInformation($"RabbitMQ connection closed.");
        }
    }
}

