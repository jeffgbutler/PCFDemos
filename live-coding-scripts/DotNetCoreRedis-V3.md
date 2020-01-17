# PCF Basics with .Net Core 3.x and Redis
This exercise will cover the following:

- Building a simple web service with .Net core
- Deploying the service to PCF
- Using Steeltoe to bind to a redis cloud instance
- Doing a blue/green deployment with PCF

## Pre-Requisites

### .Net Core

1. Install the .Net core SDK from this URL: https://dotnet.microsoft.com/download
1. Verify the install by opening a terminal or command window and typing `dotnet --version`. You should see a version string to match the version you installed

### Cloud Foundry Command Line Interface (CLI)

1. Install the Cloud Foundry CLI from this URL: https://docs.cloudfoundry.org/cf-cli/install-go-cli.html
1. Verify the install by opening a terminal or command window and typing `cf --version`. You should see a version string to match the version you installed

### Install an IDE

1. Install either Visual Studio IDE or Visual Studio Code from this URL: https://visualstudio.microsoft.com/
1. If you choose Visual Studio Code, also install the C# extension

### Obtain PCF Credentials

If you are using a private installation of PCF, then obtain credentials and API enpoint information from your PCF platform team. If you are using Pivotal Web Services (the public PCF instance hosted by Pivotal), then go to [https://run.pivotal.io/](https://run.pivotal.io/) and register for a free account.

Once you have credentials, login with the CLI...

1. Open a terminal or command window and login to PCF with the command `cf login -a api.run.pivotal.io` (or whatever API endpoint you are using if not Pivotal Web Services)
1. Enter the email you registered and the password you set

## Build a Simple Web Service

### Create the Basic Project

1. Create a basic web service project and open it in VS Code
    ```shell
    mkdir PaymentService
    cd PaymentService
    dotnet new webapi
    dotnet add package Swashbuckle.AspNetCore
    dotnet add package Microsoft.Extensions.Caching.Redis
    dotnet add package Steeltoe.Management.CloudFoundryCore
    dotnet add package Steeltoe.Extensions.Configuration.CloudFoundryCore
    dotnet add package Steeltoe.CloudFoundry.ConnectorCore
    dotnet add package Steeltoe.Extensions.Logging.DynamicLogger
    code .
    ```
1. Run the new web service with `dotnet run` (or just press F5 in Visual Studio/Code), then navigate to https://localhost:5001/WeatherForecast

1. Stop the service with `ctrl-c` (or press the stop button in Visual Studio/Code)

### Add a Payment Calculator

1. Create a new `Services` directory
1. Create a new class `PaymentService` in the `Services` directory, set its contents to the following:

    ```csharp
    using System;

    namespace PaymentService.Services
    {
        public class PaymentCalculator
        {
            public Decimal Calculate(double Amount, double Rate, int Years)
            {
                if (Rate == 0.0)
                {
                    return CalculateWithoutInterest(Amount, Years);
                }
                else
                {
                    return CalculateWithInterest(Amount, Rate, Years);
                }
            }
    
            private Decimal CalculateWithInterest(double Amount, double Rate, int Years)
            {
                double monthlyRate = Rate / 100.0 / 12.0;
                int numberOfPayments = Years * 12;
                double payment = (monthlyRate * Amount) / (1.0 - Math.Pow(1.0 + monthlyRate,
                    -numberOfPayments));
                return ToMoney(payment);
            }

            private Decimal CalculateWithoutInterest(double Amount, int Years)
            {
                int numberOfPayments = Years * 12;
                return ToMoney(Amount / numberOfPayments);
            }
    
            private Decimal ToMoney(double d)
            {
                Decimal bd = new Decimal(d);
                return Decimal.Round(bd, 2, MidpointRounding.AwayFromZero);
            }
        }
    }
    ```

### Add a Hit Counter

1. Create a new interface `IHitCountService` in the `Services` directory, set its contents to the following:

    ```csharp
    namespace PaymentService.Services
    {
        public interface IHitCountService
        {
            long GetAndIncrement();
            void Reset();
        }
    }
    ```

1. Create a new Class `MemoryHitCountService` in the `Services` directory, set its contents to the following:

    ```csharp
    namespace PaymentService.Services
    {
        public class MemoryHitCountService: IHitCountService
        {
            private long HitCount = 0;
            public long GetAndIncrement()
            {
                return ++HitCount;
            }

            public void Reset()
            {
                HitCount = 0;
            }
        }
    }
    ```


### Add a Basic Domain Object

1. Create a new `Models` directory
1. Create a new class `CalculatedPayment` in the `Models` directory, set its contents to the following:

    ```csharp
    namespace PaymentService.Models
    {
        public class CalculatedPayment
        {
            public double Amount {get; set;}
            public double Rate {get; set;}
            public int Years {get; set;}
            public decimal Payment {get; set;}
            public string Instance {get; set;}
            public long Count {get; set;}
        }
    }
    ```

### Create a REST Controller

1. Create a new class `PaymentController` in the `Controllers` directory, set its contents to the following:

    ```csharp
    using Microsoft.AspNetCore.Mvc;
    using Microsoft.Extensions.Logging;
    using Microsoft.Extensions.Options;
    using PaymentService.Models;
    using PaymentService.Services;
    using Steeltoe.Extensions.Configuration.CloudFoundry;

    namespace PaymentService.Controllers
    {
        [Route("/[controller]")]
        [ApiController]
        public class PaymentController
        {
            private PaymentCalculator PaymentCalculator;
            private CloudFoundryApplicationOptions AppOptions;
            private IHitCountService HitCountService;

            private readonly ILogger _logger;
        
            public PaymentController(PaymentCalculator paymentCalculator,
                    IOptions<CloudFoundryApplicationOptions> appOptions,
                    IHitCountService hitCountService,
                    ILogger<PaymentController> logger)
            {
                PaymentCalculator = paymentCalculator;
                AppOptions = appOptions.Value;
                HitCountService = hitCountService;
                _logger = logger;
            }

            [HttpGet]
            public ActionResult<CalculatedPayment> calculatePayment(double Amount, double Rate, int Years) {
                var Payment = PaymentCalculator.Calculate(Amount, Rate, Years);

                _logger.LogDebug("Calculated payment of {Payment} for input amount: {Amount}, rate: {Rate}, years: {Years}",
                    Payment, Amount, Rate, Years);

                return new CalculatedPayment
                {
                    Amount = Amount,
                    Rate = Rate,
                    Years = Years,
                    Instance = AppOptions.InstanceIndex.ToString(),
                    Count = HitCountService.GetAndIncrement(),
                    Payment = Payment
                };
            }
        }
    }
    ```

### Setup Dependency Injection and the MVC Pipeline

1. Modify `Startup.cs` by adding the following lines at the end of the `ConfigureServices` method:

    ```csharp
    services.AddCors();
    services.AddOptions();
    services.ConfigureCloudFoundryOptions(Configuration);
    services.AddSingleton<PaymentCalculator>();
    services.AddSingleton<IHitCountService, MemoryHitCountService>();
    ```

1. Modify `Startup.cs` by adding the following line in the `Configure` method prior to the existing line `app.UseHttpsRedirection();`:

    ```csharp
    app.UseCors(builder => builder.AllowAnyOrigin());
    ```

1. Modify `Program.cs` by modifying the `CreateHostBuilder` method so that it looks like this:

    ```csharp
    public static IHostBuilder CreateHostBuilder(string[] args) =>
        Host.CreateDefaultBuilder(args)
            .ConfigureWebHostDefaults(webBuilder =>
            {
                webBuilder.UseStartup<Startup>();
                webBuilder.UseCloudFoundryHosting();
                webBuilder.AddCloudFoundry();
            });
    ```

1. Start the application either with the debugger (F5), or by entering the command `dotnet run`

1. Try the web service with the URL https://localhost:5001/payment?amount=100000&rate=4.5&years=30

1. Verify that a payment of $506.69 is returned

## Add Swagger

Swagger is a REST documentation and UI tool, that also includes code generation tools for clients. For us, it will act as a very simple and almost free UI for the web service we've just created. There are two implementations of swagger: Swashbuckle and NSwag. For this exercise, we will use Swashbuckle.

1. Modify `Startup.cs`, add the following to the end of the `ConfigureServices` method:

    ```csharp
    services.AddSwaggerGen(c =>
    {
        c.SwaggerDoc("v1", new OpenApiInfo { Title = "My API", Version = "v1" });                
    });
    ```

1. Modify `Startup.cs`, add the following to the end of the `Configure` method:

    ```csharp
    app.UseSwagger();
    app.UseSwaggerUI(c =>
    {
        c.SwaggerEndpoint("/swagger/v1/swagger.json", "My API V1");
        c.RoutePrefix = string.Empty;
    });
    ```

1. Start the application. The swagger UI should now be available at the application root (https://localhost:5001)

1. You can also use the application at https://jeffgbutler.github.io/payment-calculator-client/ to exercise the application.

1. Notice that Swagger has documented both web services - the payment calculator we wrote as well as the default service generated by dotnet. If you want, you can delete the generated web service by deleting `WeatherForecastController.cs` and `WeatherForecast.cs`

## Deploy to Pivotal Cloud Foundry

1. Create a file `manifest.yml` in the project root directory. Set its contents to the following:

    ```yaml
    applications:
    - name: PaymentService-1.0
      random-route: true
    ```

1. `cf push`

During the push process, PCF will create a route for the app. Make note of the route - you can access the application at this URL once the application has started.

### Exercise the Application

1. Start the loan-calculator-client web page by using this URL: https://jeffgbutler.github.io/payment-calculator-client/
1. Enter the URL to your application (like https://paymentservice-10-shy-zebra.cfapps.io) in the Base URL textbox
1. Press the "Start" button. You should see random traffic being generated to the application. You should also see the all traffic is routed to instance 0
1. Scale the app by entering the command `cf scale PaymentService-1.0 -i 2` - this will request two instances of the app running. Eventually you should see traffic being routed to the two app instances. Notice that the hit count is not consistent. Why?
1. If you press the "Crash It!" button on the client page, then one of the app instances will crash. Which one depends on how the request was routed. Cloud Foundry will notice that an instance has crashed and will start a new instance automatically - this may take a few minutes to show
1. You can scale the app back down by entering `cf scale PaymentService-1.0 -i 1`

You should have noticed that the hit counter is not consistent among the instances, and that it is reset when an app instance crashes. This will demonstrate the idea of epehemeral containers and that Cloud Foundry is designed for stateless applications. We will store the hit count in an external Redis cache in a later section to correct this issue.

## Steeltoe Management Endpoints

1. Modify `Program.cs`, add the following to the `CreateHostBuilder` after the the `webBuilder.AddCloudFoundry();` call:

    ```csharp
    public static IHostBuilder CreateHostBuilder(string[] args) =>
        Host.CreateDefaultBuilder(args)
            .ConfigureWebHostDefaults(webBuilder =>
            {
                webBuilder.UseStartup<Startup>();
                webBuilder.UseCloudFoundryHosting();
                webBuilder.AddCloudFoundry();
                webBuilder.AddCloudFoundryActuators(); // added
            });
    ```

1. Modify `appsettings.json` and add the following:

    ```json
    "info": {
      "app": {
        "name": ".Net Core Payment Calculator"
      }
    }
    ```

1. Start the application. You should be able to access the management enpoints at https://localhost:5001/actuator

1. `cf push`

### Inspect the Application With the Apps Manager UI

1. Login to Pivotal Apps Manager at https://run.pivotal.io/
1. Inspect the application...specifically:
   - On the app overview page, you should see the Steeltoe logo
   - On the app overview page you should be able to inspect details of the app health
   - On the logs page you should see the recent logs for the application, and be able to change logging levels
   - On the threads page you should be able to obtain a thread dump
   - On the settings page, there should be a Steeltoe Info section. In that section you should be able to see the text you added to the info actuator by pressing the "View Raw JSON" button
   
   
## Steeltoe Service Connectors - Add a Redis Based Hit Counter

1. Create an Redis service instance in your PCF space. Name the service instance `xxxredis` where "xxx" is your initials

    - On Pivotal Web Services, add a Redis Cloud instance using the 30MB (free) plan

1. If you are using Azure Redis Cache through the Azure Service Broker on PCF, then modify `appsettings.json` and add the following configuration:

    ```json
    "redis": {
      "client": {
        "urlEncodedCredentials": true
      }
    }
    ```

1. Create a class `RedisHitCountService` in the `Services` folder. Set its contents to the following:

    ```csharp
    using StackExchange.Redis;

    namespace PaymentService.Services
    {
        public class RedisHitCountService: IHitCountService
        {
            private IConnectionMultiplexer _conn;
            public RedisHitCountService (IConnectionMultiplexer conn)
            {
                _conn = conn;
            }

            public long GetAndIncrement()
            {
                IDatabase db = _conn.GetDatabase();
                return db.StringIncrement("loan-calculator");
            }

            public void Reset()
            {
                IDatabase db = _conn.GetDatabase();
                db.StringSet("loan-calculator", 5000);
            }
        }
    }
    ```

1. Modify the constructor in `Startup.cs` to accept and keep the `IWebHostEnvironment`. The modified constructor and new instance variable look like this:

    ```csharp
    public Startup(IConfiguration configuration, IWebHostEnvironment env)
    {
        Configuration = configuration;
        Env = env;
    }

    public IWebHostEnvironment Env {get; }
    ```

1. Change the `ConfigureServices` method so that the memory based hit counter is used in the development environment and the Redis based hit counter is used in other environments:

    ```csharp
    if (Env.IsDevelopment())
    {
        services.AddSingleton<IHitCountService, MemoryHitCountService>();
    }
    else
    {
        services.AddRedisConnectionMultiplexer(Configuration);
        services.AddSingleton<IHitCountService, RedisHitCountService>();
    }
    ```

1. Modify `manifest.yml` to add the service binding (change the app version, and specify the correct name of the redis instance you created above):

    ```yaml
    applications:
    - name: PaymentService-1.1
      random-route: true
      services:
      - xxxredis
    ```

1. `cf push`

1. Exercise the application as before. You should now see the hit counter remaining consistent across instances and after crashes.

## Blue-Green Deployment

1. Create a new route with the cf cli:

    ```bash
    cf create-route dev apps.pcfpoc.jgbpcf.com --hostname xxx-loancalculator
    ```

    Where xxx is your initials

1. Assign the route to version 1.0 of the app:

    ```bash
    cf map-route NetLoanCalculator-1.0 apps.pcfpoc.jgbpcf.com --hostname xxx-loancalculator
    ```

1. Verify that the app responds to the new URL.

1. Assign the route to version 1.1 of the app:

    ```bash
    cf map-route NetLoanCalculator-1.1 apps.pcfpoc.jgbpcf.com --hostname xxx-loancalculator
    ```
    Where xxx is your initials

1. Repeatedly try the app at the new route. You should see traffic bouncing back and forth between the two versions of the app

1. Remove the route from version 1.0 of the app:

    ```bash
    cf unmap-route NetLoanCalculator-1.0 apps.pcfpoc.jgbpcf.com --hostname xxx-loancalculator
    ```

    Where xxx is your initials

1. You should now see traffic only going to the new version of the app

1. Delete version 1.0 of the app:

    ```bash
    cf delete NetLoanCalculator-1.0 -r
    ```

    This deletes the app and its route
