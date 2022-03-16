using metrics_service;

IHost host = Host.CreateDefaultBuilder(args)
    .ConfigureHostConfiguration(cfg => cfg.AddEnvironmentVariables())
    .ConfigureServices((ctx, services) =>
    {
        services.AddSingleton(ctx.Configuration);
        services.AddHostedService<Worker>();
    })
    .Build();

await host.RunAsync();