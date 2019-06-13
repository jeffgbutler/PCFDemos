# Spring Boot and Redis Exercise

## Pre-Requisites

### Cloud Foundry Command Line Interface (CLI)

1. Install the Cloud Foundry CLI from this URL: https://docs.cloudfoundry.org/cf-cli/install-go-cli.html
1. Verify the install by opening a terminal or command window and typing `cf --version`. You should see a version string to match the version you installed

### Install an IDE

Install and configure a Java IDE you are comfortable with. Good options include:

- Eclipse: https://www.eclipse.org/
- IntelliJ: https://www.jetbrains.com/idea/
- Visual Studio Code: https://visualstudio.microsoft.com/

If you install Visual Studio Code, then add the following extensions:

- (Micorsoft) Java Extension Pack
- (Pivotal) Spring Boot Extension Pack

### Obtain PCF Credentials

If you are using a private installation of PCF, then obtain credentials and API enpoint information from your PCF platform team. If you are using Pivotal Web Services (the public PCF instance hosted by Pivotal), follow thses steps:

1. Open the page [https://run.pivotal.io/](https://run.pivotal.io/) and register for a free account
1. Open a terminal or command window and login to PCF with the command `cf login -a api.run.pivotal.io` (or whatever API endpoint you are using if not Pivotal Web Services)
1. Enter the email you registered and the password you set

### Create a Redis Cache Instance

1. Login to Pivotal Apps Manager
1. Navigate to your org/space
1. Select the "services" tab
1. Press the "Add a Service" button
1. Create a new service...
   - Select "Redis Cache"
   - Select plan type "30 MB" - the free plan
   - Set the instance name to "xxxredis" where "xxx" are your initials

## Create the Basic Application

1. Navigate to [https://start.spring.io](https://start.spring.io)
1. Create a Maven project with Java and the latest version of Spring Boot (2.1.5 at the time of writing)
1. Specify group: `microservice.workshop`
1. Specify artifact: `redis-demo`
1. Specify packaging: Jar
1. Specify Java Version to match what you have installed
1. For dependencies, add the following:
    - Spring Web Starter
    - Spring Boot Actuator
    - Spring Data Redis
1. Generate the project (causes a download)
1. Unzip the downloaded file somewhere convenient
1. Add the new project to your IDE workspace
    - Eclipse: File->Import->Existing Maven Project
    - IntelliJ: File->New->Module From Existing Sources...
    - VS Code: File->Add Folder to Workspace
1. Rename `application.properties` in `src/main/resources` to `application.yml`

## Configure The Info Actuator

1. Open `application.yml` in `src/main/resources`
1. Add this value

    ```yml
    info:
      app:
        name: Payment Service

    management:
      endpoint:
        health:
          show-details: always
    ```

1. Create a file called `application-default.yml` in `src/main/resources`
1. Set its content to the following:

    ```yml
    spring:
      autoconfigure:
        exclude:
          - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
    ```

    This will tell SpringBoot not to configure Redis when we're running locally - even though Redis is on the classpath. Failure to do this will not stop the application from starting and running successfully. But the health actuator will show the application being down. 

1. Create a file called `application-cloud.yml` in `src/main/resources`
1. Set its content to the following:

    ```yml
    spring:
      autoconfigure:
        exclude:
          - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
    ```

    This will tell SpringBoot not to configure Redis when we're running in the cloud - even though Redis is on the classpath. We will remove this file once we're ready to work with redis. 

## Configure Swagger

1. Open `pom.xml`, add the following dependencies:

    ```xml
    <dependency>
      <groupId>io.springfox</groupId>
      <artifactId>springfox-swagger2</artifactId>
      <version>2.9.2</version>
    </dependency>
    <dependency>
      <groupId>io.springfox</groupId>
      <artifactId>springfox-swagger-ui</artifactId>
      <version>2.9.2</version>
    </dependency>
    ```

1. Create a class `SwaggerConfiguration` in the `micoservice.workshop.redisdemo` package. Add the following:

    ```java
    package microservice.workshop.redisdemo;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    import org.springframework.web.servlet.view.RedirectView;
    import springfox.documentation.builders.RequestHandlerSelectors;
    import springfox.documentation.spi.DocumentationType;
    import springfox.documentation.spring.web.plugins.Docket;
    import springfox.documentation.swagger2.annotations.EnableSwagger2;

    @Configuration
    @EnableSwagger2
    @Controller
    public class SwaggerConfiguration {

        @RequestMapping("/")
        public RedirectView redirectToSwagger() {
            return new RedirectView("swagger-ui.html");
        }

        @Bean
        public Docket api() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .select()
                    .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                    .build();
        }
    }
    ```

    This configuration does three important things:

    1. It enables Swagger
    1. It redirects the root URL to the Swagger UI. I find this convenient, but YMMV
    1. It tells Springfox that we only want to use Swagger for REST controllers. Without this there will be Swagger documentation for the redirect controller, as well as the basic Spring error controller and we usually don't want this.

## Create a Payment Service

1. Create a package `microservice.workshop.redisdemo.service`
1. Create a class in the new package called `PaymentService`
1. Set the content of `PaymentService` to the following:

    ```java
    package microservice.workshop.redisdemo.service;

    import java.math.BigDecimal;
    import java.math.RoundingMode;

    public class PaymentService {

        public BigDecimal calculate(double amount, double rate, int years) {
            if (rate == 0.0) {
                return calculateWithoutInterest(amount, years);
            } else {
                return calculateWithInterest(amount, rate, years);
            }
        }

        private BigDecimal calculateWithInterest(double amount, double rate, int years) {
            double monthlyRate = rate / 100.0 / 12.0;
            int numberOfPayments = years * 12;
            double payment = (monthlyRate * amount) / (1.0 - Math.pow(1.0 + monthlyRate, -numberOfPayments));
            return toMoney(payment);
        }

        private BigDecimal calculateWithoutInterest(double amount, int years) {
            int numberOfPayments = years * 12;
            return toMoney(amount / numberOfPayments);
        }

        private BigDecimal toMoney(double d) {
            BigDecimal bd = new BigDecimal(d);
            return bd.setScale(2, RoundingMode.HALF_UP);
        }
    }
    ```
## Create a Hit Counter Service

1. Create an interface in the `microservice.workshop.redisdemo.service` package called `HitCounterService`
1. Set the content of `HitCounterService` to the following:

    ```java
    package microservice.workshop.redisdemo.service;

    public interface HitCounterService {
        long incrementCounter();
        void resetCount();
    }
    ```

1. Create a class in the `microservice.workshop.redisdemo.service` package called `MemoryHitCounterService`
1. Set the content of `MemoryHitCounterService` to the following:

    ```java
    package microservice.workshop.redisdemo.service;

    public class MemoryHitCounterService implements HitCounterService {

        private long hitCount = 0;
    
        @Override
        public long incrementCounter() {
            return ++hitCount;
        }

        @Override
        public void resetCount() {
            hitCount = 0;
        }
    }
    ```

## Create a Return Model

1. Create a package `microservice.workshop.redisdemo.model`
1. Create a class in the new package called `CalculatedPayment`
1. Set the content of `CalculatedPayment` to the following:

    ```java
    package microservice.workshop.redisdemo.model;

    import java.math.BigDecimal;

    public class CalculatedPayment {
        private double amount;
        private double rate;
        private int years;
        private BigDecimal payment;
        private String instance;
        private Long count;
    
        // getters/setters
    }
    ```

## Create a REST Controller for the Payment Service

1. Create a package `microservice.workshop.redisdemo.http`
1. Create a class in the new package called `PaymentController`
1. Set the content of `PaymentController` to the following:

    ```java
    package microservice.workshop.redisdemo.http;

    import java.math.BigDecimal;

    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.web.bind.annotation.CrossOrigin;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestParam;
    import org.springframework.web.bind.annotation.RestController;

    import microservice.workshop.redisdemo.model.CalculatedPayment;
    import microservice.workshop.redisdemo.service.HitCounterService;
    import microservice.workshop.redisdemo.service.PaymentService;

    @CrossOrigin(origins = "*")
    @RestController
    @RequestMapping("/payment")
    public class PaymentController {

        @Value("${cloud.application.instance_index:local}")
        private String instance;

        @Autowired
        private HitCounterService hitCounterService;

        @Autowired
        private PaymentService paymentService;

        private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

        @GetMapping()
        public CalculatedPayment calculatePayment(@RequestParam("amount") double amount, @RequestParam("rate") double rate,
                @RequestParam("years") int years) {

            BigDecimal payment = paymentService.calculate(amount, rate, years);

            logger.debug("Calculated payment of {} for input amount: {}, rate: {}, years: {}",
                payment, amount, rate, years);

            CalculatedPayment calculatedPayment = new CalculatedPayment();
            calculatedPayment.setAmount(amount);
            calculatedPayment.setRate(rate);
            calculatedPayment.setYears(years);
            calculatedPayment.setPayment(payment);
            calculatedPayment.setCount(hitCounterService.incrementCounter());
        
            return calculatedPayment;
        }
    }
    ```
## Create a REST Controller to Reset the Hit Count

This is needed for the unit tests - will reset the hit counter to a known state for each test.

1. Create a class `ResetHitCounterController` in package `microservice.workshop.redisdemo.http`
1. Set the content of `ResetHitCounterController` to the following:

    ```java
    package microservice.workshop.redisdemo.http;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.web.bind.annotation.CrossOrigin;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;

    import microservice.workshop.redisdemo.service.HitCounterService;

    @CrossOrigin(origins = "*")
    @RestController
    @RequestMapping("/resetCount")
    public class ResetHitCounterController {

        @Autowired
        private HitCounterService hitCounterService;
    
        @GetMapping
        public void reset() {
            hitCounterService.resetCount();
        }
    }
    ```

## Setup Dependency Injection

1. Open the class `RedisDemoApplication` in the package `microservice.workshop.redisdemo`
1. Add the following methods:

    ```java
    @Bean
    public PaymentService paymentService() {
        return new PaymentService();
    }
    
    @Bean
    public HitCounterService hitCounterService() {
        return new MemoryHitCounterService();
    }
    ```

## Unit Tests

1. Make a new package `microservice.workshop.redisdemo.http` in the `src/test/java` tree
1. Create a class in the new package called `PaymentControllerTest`
1. Set the content of `PaymentControllerTest` to the following:

    ```java
    package microservice.workshop.redisdemo.http;

    import static org.hamcrest.Matchers.*;
    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
    import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

    import org.junit.Before;
    import org.junit.Test;
    import org.junit.runner.RunWith;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.test.context.SpringBootTest;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.MediaType;
    import org.springframework.test.context.junit4.SpringRunner;
    import org.springframework.test.web.servlet.MockMvc;
    import org.springframework.web.context.WebApplicationContext;

    @RunWith(SpringRunner.class)
    @SpringBootTest
    public class PaymentControllerTest {
        private MockMvc mockMvc;
    
        @Autowired
        private WebApplicationContext webApplicationContext;

        @Before
        public void setup() {
            this.mockMvc = webAppContextSetup(webApplicationContext).build();
        }

        @Test
        public void testWithInterest() throws Exception {
            mockMvc.perform(get("/resetCount"))
            .andExpect(status().is(HttpStatus.OK.value()));
            
            mockMvc.perform(get("/payment?amount=100000&rate=3.5&years=30"))
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$.payment", is(449.04)))
            .andExpect(jsonPath("$.count", is(1)));
        }

        @Test
        public void testZeroInterest() throws Exception {
            mockMvc.perform(get("/resetCount"))
            .andExpect(status().is(HttpStatus.OK.value()));
            
            mockMvc.perform(get("/payment?amount=100000&rate=0&years=30"))
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$.payment", is(277.78)))
            .andExpect(jsonPath("$.count", is(1)));
        }

        @Test
        public void testThatHitCounterIncrements() throws Exception {
            mockMvc.perform(get("/resetCount"))
            .andExpect(status().is(HttpStatus.OK.value()));
            
            mockMvc.perform(get("/payment?amount=100000&rate=3.5&years=30"))
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$.payment", is(449.04)))
            .andExpect(jsonPath("$.count", is(1)));

            mockMvc.perform(get("/payment?amount=100000&rate=0&years=30"))
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$.payment", is(277.78)))
            .andExpect(jsonPath("$.count", is(2)));
        }
    }
    ```

## Testing

1. Run the unit tests:
    - (Windows Command Prompt) `mvnw clean test`
    - (Windows Powershell) `.\mvnw clean test`
    - (Mac/Linux) `./mvnw clean test`
    - Or your IDE's method of running tests

1. Start the application:
    - (Windows Command Prompt) `mvnw spring-boot:run`
    - (Windows Powershell) `.\mvnw  spring-boot:run`
    - (Mac/Linux) `./mvnw  spring-boot:run`
    - Or your IDE's method of running the main application class

1. Test Swagger [http://localhost:8080](http://localhost:8080)
1. Test the acuator health endpoint [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
1. Test the acuator info endpoint [http://localhost:8080/actuator/info](http://localhost:8080/actuator/info)

## Deploy to Cloud Foundry

1. Create a file `manifest.yml` in the project root directory. Set it's contents to the following:

    ```yaml
    applications:
    - name: RedisDemo-1.0
      path: target/redis-demo-0.0.1-SNAPSHOT.jar
      random-route: true
      env:
        JBP_CONFIG_OPEN_JDK_JRE: '{jre: {version: 12.+}}'
    ```

    Note that this manifest specifies JRE 12+. This is not required if you are using Java 8. If you are using Java 11, you can change the environment variable accordingly. 

1. Build the application:
    - (Windows Command Prompt) `mvnw clean package`
    - (Windows Powershell) `.\mvnw  clean package`
    - (Mac/Linux) `./mvnw  clean package`
    - Or your IDE's method of running the Maven build

1. `cf push`

You should now be able to test the app with Swagger at the route created by PCF. One thing to note is that the hit counter will reset everytime you deploy (because it is stored in the app state). Also, when the app is scaled the hit counter will not be shared across instances.

## Configure Redis on Cloud Foundry

1. Create a new Redis instance
    1. Login to Pivotal Apps Manager
    1. Navigate to your org/space
    1. Select the "services" tab
    1. Press the "Add a Service" button
    1. Create a new service...

1. Create a new class `RedisHitCounterService` in the `microservice.workshop.redisdemo.service` package
1. Set the contents of `RedisHitCounterService` to the following:

    ```java
    package microservice.workshop.redisdemo.service;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.data.redis.core.RedisTemplate;

    public class RedisHitCounterService implements HitCounterService {

        private static final String REDIS_KEY = "payment-calculator";

        @Autowired
        private RedisTemplate<String, Integer> redisTemplate;
    
        @Override
        public long incrementCounter() {
            return redisTemplate.opsForValue().increment(REDIS_KEY, 1);
        }

        @Override
        public void resetCount() {
            redisTemplate.opsForValue().set(REDIS_KEY, 5000);
        }
    }
    ```

1. Create a class `CloudConfiguration` in the `micoservice.workshop.redisdemo` package. Add the following:

    ```java
    package microservice.workshop.redisdemo;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.context.annotation.Profile;
    import org.springframework.data.redis.connection.RedisConnectionFactory;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.data.redis.serializer.GenericToStringSerializer;

    import microservice.workshop.redisdemo.service.HitCounterService;
    import microservice.workshop.redisdemo.service.RedisHitCounterService;

    @Configuration
    @Profile("cloud")
    public class CloudConfiguration {

        @Bean
        public RedisTemplate<String, Integer> redisTemplate(RedisConnectionFactory redisFactory) {
            RedisTemplate<String, Integer> template = new RedisTemplate<>();
            template.setConnectionFactory(redisFactory;
            template.setValueSerializer(new GenericToStringSerializer<>(Integer.class));
            return template;
        }
    
        @Bean
        public HitCounterService hitCounterService() {
            return new RedisHitCounterService();
        }
    }
    ```

    This configuration is enabled when the "cloud" profile is enabled only. On Cloud Foundry, the Java build pack enables this profile. When enabled, this configuration will create a Redis connection based on the Redis instance bound to the application.

1. Open `RedisDemoApplication` in the `microservice.workshop.redisdemo` package
1. Change the `hitCounterService` method so that it is only active when not on the cloud:

    ```java
    @Bean
    @Profile("!cloud")
    public HitCounterService hitCounterService() {
        return new MemoryHitCounterService();
    }
    ```

1. Modify the file `manifest.yml` in the project root directory. Set it's contents to the following:

    ```yaml
    applications:
    - name: RedisDemo-1.1
      path: target/redis-demo-0.0.1-SNAPSHOT.jar
      random-route: true
      services:
      - xxxredis
    ```

    Where "xxxredis" maps to the name of the redis instance you created above

1. Build the application:
    - (Windows Command Prompt) `mvnw clean package`
    - (Windows Powershell) `.\mvnw  clean package`
    - (Mac/Linux) `./mvnw  clean package`
    - Or your IDE's method of running the Maven build

1. `cf push`

You should now be able to test the app with Swagger at the route created by PCF. The hit counter will now persist across deploymant and will be consistent as the app scales.
