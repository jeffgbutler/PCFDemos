# Spring Boot and Wavefront Exercise

Follow the steps in the [Java and Redis](JavaRedis.MD) demo up to, but not including, the point where the app is deployed to Cloud Foundry - it is not necessary to deploy the app to cloud foundry for this demo.

Documentation about using Wavefront with Spring Boot: https://docs.wavefront.com/wavefront_springboot.html

## Basic Wavefront Configuration

1. Open `pom.xml`, add the following dependencies:

    ```xml
    <!-- additions for Wavefront -->
    <dependency>
      <groupId>com.wavefront</groupId>
      <artifactId>wavefront-spring-boot-starter</artifactId>
      <version>2.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>io.opentracing.contrib</groupId>
      <artifactId>opentracing-spring-cloud-starter</artifactId>
      <version>0.5.4</version>
    </dependency>
    <!-- end additions for Wavefront -->
    ```
1. You might need to configure access to the Spring snapshots by adding the following to `pom.xml`:

    ```xml
    <!-- additions for Wavefront -->
    <repositories>
      <repository>
        <id>spring-snapshots</id>
        <name>Spring Snapshots</name>
        <url>https://repo.spring.io/snapshot</url>
        <snapshots>
          <enabled>true</enabled>
        </snapshots>
      </repository>
    </repositories>
    <!-- end additions for Wavefront -->
    ```

This is all you need to do to add basic Wavefront support to a Spring Boot application. You can run the application and test the integration by following these steps:

1. Start the application:
    - (Windows Command Prompt) `mvnw spring-boot:run`
    - (Windows Powershell) `.\mvnw  spring-boot:run`
    - (Mac/Linux) `./mvnw  spring-boot:run`
    - Or your IDE's method of running the main application class
1. Notice during startup that Wavefront integration will be automatically configured. You will see lines similar to these in the log output:

    ```
    Your existing Wavefront account information has been restored from disk.

    To share this account, make sure the following is added to your configuration:

	    management.metrics.export.wavefront.api-token=*****-****-****-****-********
	    management.metrics.export.wavefront.uri=https://wavefront.surf

    Connect to your Wavefront dashboard using this one-time use link:
    https://wavefront.surf/us/********
    ```

1. Start the loan-calculator-client web page by using this URL: https://jeffgbutler.github.io/payment-calculator-client/
1. Enter the URL to your application (like https://localhost:8080) in the Base URL textbox
1. Press the "Start" button. You should see random traffic being generated for the application.
1. Follow the link to Wavefront from the log output - you should see a wavefront dashboard for the application. It may take a few minutes for data to appear in the Wavefront dashboard.
1. Stop the client web app and the Spring Boot application

## Wavefront Customization

You will notice that the Wavefront dashboard shows data four your application as "unnamed_application" and "unnamed_service". We can fix this by adding Wavefront configuration to the application properties.

1. Open `src/main/resources/application.yml`
1. Add the following values:

    ```yaml
    wavefront:
      application:
        name: Microservice-Workshop
        service: Payment-Service
    ```

    This will register that application name as "Microservice-Workshop" and service name as "Payment-Service". Note there are two levels of naming - application and service. This shows that Wavefront understands microservices based systems. Each microservice in an application can share the same application name, but use a different service name. This will enable Wavefront to understand the microservices that make up an application.

You may also find that copying the URL from the log output is a cumbersome way of getting to the Wavefront dashboard. The Spring actuators can help.

1. Open `src/main/resources/application.yml`
1. Add the following values:

    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include:
            - info
            - health
            - wavefront
    ```

    This will enable the Wavefront actuator (in addition to "info" and "health" which are enabled by default). The Wavefront actuator will redirect from `/actuator/wavefront` to the Wavefront dashboard for the application.

1. Start the Spring Boot application
1. Start the client application
1. Navigate to Wavefront through `http://localhost:8080/actuator/wavefront`
1. Notice that data for the application is now grouped into a specific application rather than the catch all of "unnamed application"
1. Stop the client web app and the Spring Boot application

## Error Diagnosis

Wavefront can be very useful debug tool for developers. It will captures errors and their associated log messages. We will change the application so that it throws some errors, and then see how Wavefront can be used to find and diagnose errors.

1. Open `PaymentController.java` in the `microservice.workshop.redisdemo.http` package
1. Add the following lines to the top of the `calculatePayment` method:

    ```java
    if (amount > 700000) {
        logger.error("Amounts over $700,000 are not allowed. {} requested.", amount);
        throw new RuntimeException("Amounts over $700,000 are not allowed.");
    }
    ```

    This will cause an error whenever a loan amount over $700,000 is requested

1. Start the Spring Boot application
1. Start the client application
1. Navigate to Wavefront through `http://localhost:8080/actuator/wavefront`
1. Wait for some errors to show in the Wavefront dashboard, then use the Wavefront tracing feature to show the logs
1. Stop the client web app and the Spring Boot application

## Custom Metrics

Wavefront can track custom metrics. We can add any number of custom metrics to an application and emit them to Wavefront.

1. Open `PaymentController.java` in the `microservice.workshop.redisdemo.http` package
1. Add the following constructor and instance variable:

  ```java
  private Counter counter;

  public PaymentController(MeterRegistry registry) {
      counter = registry.counter("paymentservice.calculatepayment.count");
  }
  ```

  `Counter` and `MeterRegistry` are in the `io.micrometer.core.instrument` package

1. Add the following line in the `calculatePayment` method:

  ```java
  counter.increment();
  ```

1. Open `ResetHitCounterController.java` in the `microservice.workshop.redisdemo.http` package

1. Add the following constructor and instance variable:

  ```java
  private Counter counter;

  public ResetHitCounterController(MeterRegistry registry) {
    counter = registry.counter("paymentservice.reset.count");
  }
  ```

  `Counter` and `MeterRegistry` are in the `io.micrometer.core.instrument` package

1. Add the following line in the `reset` method:

  ```java
  counter.increment();
  ```

1. Start the Spring Boot application
1. Start the client application
1. Navigate to Wavefront through `http://localhost:8080/actuator/wavefront`
1. Show the custom metrics in tWavefront
1. Stop the client web app and the Spring Boot application
