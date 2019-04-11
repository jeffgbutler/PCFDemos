# PCF Basics with .Net Core and Redis
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

1. Install either Visual Studio IDE or Visual Studio Code fomr this URL: https://visualstudio.microsoft.com/
1. If you choose Visual Studio Code, also install the C# extension

### Obtain PCF Credentials

If you are using your own installation of PCF, then obtain credentials and API enpoint information from your PCF platform team. If you are using Pivotal Web Services (the public PCF instance hosted by Pivotal), follow thses steps:

1. Open the page https://run.pivotal.io/ and register for a free account
1. Open a terminal or command window and login to PCF with the command `cf login -a api.run.pivotal.io` (or whatever API endpoint you are using if not Pivotal Web Services)
1. Enter the email you registered with and the password you set

### Create a Redis Cache Instance

1. Login to Pivotal Apps Manager
1. Navigate to your org/space
1. Select the "services" tab
1. Press the "Add a Service" button
1. Create a new service...
   - (With the Azure Service Broker)
   - Select "Azure Redis Cache"
   - Select plan type "Basic C0"
   - name the instance "xxxredis" where "xxx" are your initials

## Build a Simple Web Service

1. Create a basic web service project and open it in VS Code
    ```shell
    mkdir PaymentService
    cd PaymentService
    dotnet new webapi
    code .
    ```
1. Run the new web service with `dotnet run`, then navigate to https://localhost:5001/api/values

1. Stop the service with `ctrl-c`

1. Create a new "Services" directory, then add this class:

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

1. Create a new "Models" directory, then add this class:

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

1. Create this class in the "Controllers" directory:

    ```csharp
    using System;
    using Microsoft.AspNetCore.Mvc;
    using PaymentService.Models;
    using PaymentService.Services;

    namespace PaymentService.Controllers
    {
        [Route("/[controller]")]
        [ApiController]
        public class PaymentController
        {
            private PaymentCalculator _PaymentCalculator;
    
            public PaymentController(PaymentCalculator PaymentCalculator)
            {
                _PaymentCalculator = PaymentCalculator;
            }

            [HttpGet]
            public ActionResult<CalculatedPayment> calculatePayment(double Amount, double Rate, int Years)
            {
                CalculatedPayment rv = new CalculatedPayment();
                rv.Amount = Amount;
                rv.Rate = Rate;
                rv.Years = Years;
                rv.Instance = Environment.GetEnvironmentVariable("CF_INSTANCE_INDEX");
                rv.Payment = _PaymentCalculator.Calculate(Amount, Rate, Years);

                return rv;
            }
        }
    }
    ```
1. Modify `Startup.cs` by adding the following line at the end of the `ConfigureServices` method:

    ```csharp
    services.AddSingleton<PaymentCalculator>();
    ```

1. Start the application either with the debugger (F5), or by entering the command `dotnet run`

1. Try the web service with the URL https://localhost:5001/payment?amount=100000&rate=4.5&years=30

1. Verify that a payment of $506.69 is returned

## Add Swagger

Swagger is a REST documentation and UI tool, that also includes code generation tools for clients. For us, it will act as a very simple and almost free UI for the web service we've just created. There are two implementations of swagger: Swashbuckle and NSwag. For this exercise, we will use Swashbuckle.

1. Add the Nuget package for Swashbuckle to the project:
    ```shell
    dotnet add package Swashbuckle.AspNetCore
    ```

1. Modify `Startup.cs`, add the following to the end of the `ConfigureServices` method:

    ```csharp
    services.AddSwaggerGen(c =>
    {
        c.SwaggerDoc("v1", new Info { Title = "My API", Version = "v1" });                
    });
    ```

1. Modify `Startup.cs`, add the following to the end of the `Configure` method before the `UseMvc` middleware:

    ```csharp
    app.UseSwagger();
    app.UseSwaggerUI(c =>
    {
        c.SwaggerEndpoint("/swagger/v1/swagger.json", "My API V1");
        c.RoutePrefix = string.Empty;
    });
    ```

1. Start the application. The swagger UI should now be available at the application root (https://localhost:5001)

1. Notice that Swagger has documented both web services - the payment calculator we wrote as well as the default service generated by dotnet. If you want, you can delete the generated web service by deleting `ValuesController.cs`

## Deploy to Pivotal Cloud Foundry

1. Create a file `manifest.yml` in the project root directory. Set it's contents to the following:

    ```yaml
    applications:
    - name: PaymentService-1.0
      path: bin/Debug/netcoreapp2.2/publish
     random-route: true
    ```

1. `dotnet publish`

1. `cf push`

During the push process, PCF will create a route for the app. Make note of the route - you can acces the application at this URL once the application has started.

## Steeltoe Management Endpoints

1. Add the Nuget package for Steeltoe to the project:
    ```shell
    dotnet add package Steeltoe.Management.CloudFoundryCore
    dotnet add package Steeltoe.Extensions.Configuration.CloudFoundryCore
    ```

1. Modify `Startup.cs`, add the following to the end of the `ConfigureServices` method:

    ```csharp
    services.AddCloudFoundryActuators(Configuration);
    ```

1. Modify `Startup.cs`, add the following to the end of the `Configure` method before the Swagger middleware:

    ```csharp
    app.UseCloudFoundryActuators();
    ```

1. Modify `Program.cs`, add the following to the `CreateWebHostBuilder` after the `UseStartup` call:

    ```csharp
    .UseStartup<Startup>() // existing
    .AddCloudFoundry()
    .ConfigureLogging((builderContext, loggingBuilder) =>
    {
        loggingBuilder.AddConfiguration(builderContext.Configuration.GetSection("Logging"));
        loggingBuilder.AddDynamicConsole();
        loggingBuilder.AddDebug();
    });
    ```

1. Modify `appsettings.json` and add the following configuration:

    ```json
    "management": {
      "endpoints": {
        "path": "/cloudfoundryapplication",
        "cloudfoundry": {
          "validateCertificates": false
        }
      }
    } 
    ```

1. `dotnet publish`

1. `cf push`

## Steeltoe Service Connectors Part 1 - Add a Basic Hit Counter

1. Add the Nuget package for Redis to the project:
    ```shell
    dotnet add package Microsoft.Extensions.Caching.Redis
    ```
1. If you are using Azure Redis Cache through the Azure Service Broker on PCF, then modify `appsettings.json` and add the following configuration:

    ```json
    "redis": {
      "client": {
        "urlEncodedCredentials": true
      }
    }
    ```

1. Create a file called `HitCounter.cs` in the `Services` folder. Set it's contents to the following:

    ```csharp
    using StackExchange.Redis;

    namespace PaymentService.Services
    {
        public interface IIHitCountService
        {
            long GetAndIncrement();
            void Reset();
        }

        public class MemoryHitCountService: IIHitCountService
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

        public class RedisHitCountService: IIHitCountService
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

1. Modify the constructor in `Startup.cs` to accept and keep the `IHostingEnvironment`

1. Add the following to the `ConfigureServices` method:

    ```csharp
    if (env.IsDevelopment())
    {
        services.AddSingleton<IIHitCountService, MemoryHitCountService>();
    }
    else
    {
        services.AddSingleton<IIHitCountService, RedisHitCountService>();
    }
    ```

1. Modify the constructor for `PaymentController` to accept and keep an instance of `IHitCountService`

1. Modify the `calculatePayment` method in `PaymentController` to set the hit count into the result object:

    ```csharp
    rv.Count = _HitCountService.GetAndIncrement();
    ```

1. Test the application locally with swagger to make sure the hit count is incrementing

## Steeltoe Service Connectors Part 2 - Bind the Application to Redis on Cloud Coundry

1. Add the Nuget package for Steeltoe Connectors to the project:
    ```shell
    dotnet add package Steeltoe.CloudFoundry.ConnectorCore
    ```

1. Add the following to the `ConfigureServices` method:

    ```csharp
    services.AddRedisConnectionMultiplexer(Configuration);
    ```

1. Modify `manifest.yml` to add the service binding (change the app version, and specify the correct name of the redis instance you created above):

    ```yaml
    applications:
    - name: PaymentService-1.1
      path: bin/Debug/netcoreapp2.2/publish
      random-route: true
      services:
      - xxxredis
    ```

1. `dotnet publish`

1. `cf push`

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

1. You should now see traffic only koing to the new version of the app

1. Delete version 1.0 of the app:

    ```bash
    cf delete NetLoanCalculator-1.0 -r
    ```

    This deletes the app and its route
