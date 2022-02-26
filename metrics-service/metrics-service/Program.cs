using metrics_service;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

IHost host = Host.CreateDefaultBuilder(args)
    .ConfigureHostConfiguration(cfg => cfg.AddEnvironmentVariables())
    .ConfigureServices((ctx, services) =>
    {
        services.AddSingleton(ctx.Configuration);
        services.AddHostedService<Worker>();
    })
    .Build();

await host.RunAsync();