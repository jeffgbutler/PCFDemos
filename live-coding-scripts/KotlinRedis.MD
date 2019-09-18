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

- (Microsoft) Java Extension Pack
- (Pivotal) Spring Boot Extension Pack

### Obtain PCF Credentials

If you are using a private installation of PCF, then obtain credentials and API enpoint information from your PCF platform team. If you are using Pivotal Web Services (the public PCF instance hosted by Pivotal), then go to [https://run.pivotal.io/](https://run.pivotal.io/) and register for a free account.

Once you have credentials, login with the CLI...

1. Open a terminal or command window and login to PCF with the command `cf login -a api.run.pivotal.io` (or whatever API endpoint you are using if not Pivotal Web Services)
1. Enter the email you registered and the password you set

## Create the Basic Application

1. Navigate to [https://start.spring.io](https://start.spring.io)
1. Create a Gradle project with Kotlin and the latest version of Spring Boot (2.1.8 at the time of writing)
1. Specify group: `microservice.workshop`
1. Specify artifact: `kotlin-redis-demo`
1. Specify packaging: Jar
1. Specify Java Version to match what you have installed
1. For dependencies, add the following:
    - Spring Web
    - Spring Boot Actuator
    - Spring Data Redis
1. Generate the project (causes a download)
1. Unzip the downloaded file somewhere convenient
1. Add the new project to your IDE workspace
    - Eclipse: File->Import->Existing Gradle Project
    - IntelliJ: File->New->Module From Existing Sources...
    - VS Code: File->Add Folder to Workspace (or just open the folder by navigating to it and entering the command `code .`)

## Configure The Info Actuator

1. Rename `application.properties` in `src/main/resources` to `application.yml`
1. Open `application.yml` in `src/main/resources`
1. Add this value

    ```yml
    info:
      app:
        name: Kotlin Payment Service

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
    management:
      endpoints:
        web:
          exposure:
            include: '*'
    ```

    This will tell SpringBoot not to configure Redis when we're running locally - even though Redis is on the classpath. Failure to do this will not stop the application from starting and running successfully. But the health actuator will show the application being down. This also enables all the actuators when running locally.

1. Create a file called `application-cloud.yml` in `src/main/resources`
1. Set its content to the following:

    ```yml
    spring:
      autoconfigure:
        exclude:
          - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
    ```

    This will tell SpringBoot not to configure Redis when we're running in the cloud - even though Redis is on the classpath. We will remove this file once we're ready to work with Redis. 

## Configure Swagger

1. Open `build.gradle.kts`, add the following plugin, Spring Boot configuration, and dependencies:

    ```kotlin
    plugins {
        // existing
      	id("com.gorylenko.gradle-git-properties") version "2.1.0"
    }

    // new...
    springBoot {
    	buildInfo()
    }

    dependencies {
        // exitsing
    	implementation("io.springfox:springfox-swagger2:2.9.2")
	    implementation("io.springfox:springfox-swagger-ui:2.9.2")
    }
    ```

1. Create a class `SwaggerConfiguration` in the `micoservice.workshop.kotlinredisdemo` package. Set its contents to the following:

    ```kotlin
    package microservice.workshop.kotlinredisdemo

    import org.springframework.context.annotation.Bean
    import org.springframework.context.annotation.Configuration
    import org.springframework.stereotype.Controller
    import org.springframework.web.bind.annotation.RequestMapping
    import org.springframework.web.bind.annotation.RestController
    import org.springframework.web.servlet.view.RedirectView
    import springfox.documentation.builders.RequestHandlerSelectors
    import springfox.documentation.spi.DocumentationType
    import springfox.documentation.spring.web.plugins.Docket
    import springfox.documentation.swagger2.annotations.EnableSwagger2

    @Configuration
    @EnableSwagger2
    @Controller
    class SwaggerConfiguration {
        @RequestMapping("/")
        fun redirectToSwagger() =
            RedirectView("swagger-ui.html")

        @Bean
        fun api(): Docket =
            Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController::class.java))
                .build()
    }
    ```

    This configuration does three important things:

    1. It enables Swagger
    1. It redirects the root URL to the Swagger UI. I find this convenient, but YMMV
    1. It tells Springfox that we only want to use Swagger for REST controllers. Without this there will be Swagger documentation for the redirect controller, as well as the basic Spring error controller and we usually don't want this.

## Create a Payment Service

1. Create a package `microservice.workshop.kotlinredisdemo.service`
1. Create a class in the new package called `PaymentService`
1. Set the content of `PaymentService` to the following:

    ```kotlin
    package microservice.workshop.kotlinredisdemo.service

    import java.math.BigDecimal
    import java.math.RoundingMode

    class PaymentService {
        fun calculate(amount: Double, rate: Double, years: Int) =
            if (rate == 0.0) {
                calculateWithoutInterest(amount, years)
            } else {
                calculateWithInterest(amount, rate, years)
            }

        private fun calculateWithInterest(amount: Double, rate: Double, years: Int): BigDecimal {
            val monthlyRate = rate / 100.0 / 12.0
            val numberOfPayments = years * 12
            val payment = monthlyRate * amount / (1.0 - Math.pow(1.0 + monthlyRate, (-numberOfPayments).toDouble()))
            return toMoney(payment)
        }

        private fun calculateWithoutInterest(amount: Double, years: Int): BigDecimal {
            val numberOfPayments = years * 12
            return toMoney(amount / numberOfPayments)
        }

        private fun toMoney(d: Double) =
            BigDecimal(d).setScale(2, RoundingMode.HALF_UP)
    }
    ```

## Create a Hit Counter Service

1. Create a file in the `microservice.workshop.kotlinredisdemo.service` package called `HitCounterService.kt`
1. Set the contents of `HitCounterService.kt` to the following:

    ```kotlin
    package microservice.workshop.kotlinredisdemo.service

    interface HitCounterService {
        fun incrementCounter(): Long
        fun resetCount()
    }

    class MemoryHitCounterService: HitCounterService {
        private var hitCount: Long = 0

        override fun incrementCounter() = ++hitCount

        override fun resetCount() {
            hitCount = 0
        }
    }
    ```

## Create a Crash Service

1. Create a class in the `microservice.workshop.kotlinredisdemo.service` package called `CrashService`
1. Set the content of `CrashService` to the following:

    ```kotlin
    package microservice.workshop.kotlinredisdemo.service

    import java.util.concurrent.*
    import kotlin.system.exitProcess

    class CrashService {
        private val executor = Executors.newScheduledThreadPool(1)

        // calls System.exit after a 2 second delay
        fun crashIt() {
            executor.schedule({ exitProcess(22) }, 2000, TimeUnit.MILLISECONDS)
        }
    }
    ```

## Create a Return Model

1. Create a package `microservice.workshop.kotlinredisdemo.model`
1. Create a class in the new package called `CalculatedPayment`
1. Set the content of `CalculatedPayment` to the following:

    ```kotlin
    package microservice.workshop.kotlinredisdemo.model

    import java.math.BigDecimal

    data class CalculatedPayment(
        val amount: Double,
        val rate: Double,
        val years: Int,
        val payment: BigDecimal,
        val instance: String,
        val count: Long
    )
    ```

## Create a REST Controller for the Payment Service

1. Create a package `microservice.workshop.kotlinredisdemo.http`
1. Create a class in the new package called `PaymentController`
1. Set the content of `PaymentController` to the following:

    ```kotlin
    package microservice.workshop.kotlinredisdemo.http

    import microservice.workshop.kotlinredisdemo.model.CalculatedPayment
    import microservice.workshop.kotlinredisdemo.service.HitCounterService
    import microservice.workshop.kotlinredisdemo.service.PaymentService
    import org.slf4j.LoggerFactory
    import org.springframework.beans.factory.annotation.Value
    import org.springframework.web.bind.annotation.*

    @CrossOrigin(origins = ["*"])
    @RestController
    @RequestMapping("/payment")
    class PaymentController(private val hitCounterService: HitCounterService, private val paymentService: PaymentService) {
        @Value("\${cloud.application.instance_index:local}")
        private lateinit var instance: String

        @GetMapping
        fun calculatePayment(@RequestParam("amount") amount: Double, @RequestParam("rate") rate: Double,
                             @RequestParam("years") years: Int): CalculatedPayment {

            val payment = paymentService.calculate(amount, rate, years)

            logger.debug("Calculated payment of {} for input amount: {}, rate: {}, years: {}",
                payment, amount, rate, years)

            return CalculatedPayment(amount, rate, years, payment, instance, hitCounterService.incrementCounter())
        }

        companion object {
            val logger = LoggerFactory.getLogger(PaymentController::class.java)
        }
    }
    ```
## Create a REST Controller to Reset the Hit Count

This is needed for the unit tests - it will reset the hit counter to a known state for each test.

1. Create a class `ResetHitCounterController` in package `microservice.workshop.kotlinredisdemo.http`
1. Set the content of `ResetHitCounterController` to the following:

    ```kotlin
    package microservice.workshop.kotlinredisdemo.http

    import microservice.workshop.kotlinredisdemo.service.HitCounterService
    import org.springframework.web.bind.annotation.CrossOrigin
    import org.springframework.web.bind.annotation.GetMapping
    import org.springframework.web.bind.annotation.RequestMapping
    import org.springframework.web.bind.annotation.RestController

    @CrossOrigin(origins = ["*"])
    @RestController
    @RequestMapping("/resetCount")
    class ResetHitCounterController(private val hitCounterService: HitCounterService) {

        @GetMapping
        fun reset() {
            hitCounterService.resetCount()
        }
    }
    ```

## Create a REST Controller to Crash the Application

This is needed to demonstrate Cloud Foundriy's self-healing capabilities.

1. Create a class `CrashController` in package `microservice.workshop.kotlinredisdemo.http`
1. Set the content of `CrashController` to the following:

    ```kotlin
    package microservice.workshop.kotlinredisdemo.http

    import io.swagger.annotations.ApiOperation
    import microservice.workshop.kotlinredisdemo.service.CrashService
    import org.springframework.web.bind.annotation.CrossOrigin
    import org.springframework.web.bind.annotation.GetMapping
    import org.springframework.web.bind.annotation.RequestMapping
    import org.springframework.web.bind.annotation.RestController

    @CrossOrigin(origins = ["*"])
    @RestController
    @RequestMapping("/crash")
    class CrashController(private val crashService: CrashService) {

        @ApiOperation("Warning! The application will crash 2 seconds after this method is called")
        @GetMapping
        fun crashIt(): String {
            crashService.crashIt()
            return "OK"
        }
    }
    ```

## Setup Dependency Injection

1. Open the file `KotlinRedisDemoApplication.kt` in the package `microservice.workshop.kotlinredisdemo`
1. Add the following methods to the `KotlinRedisDemoApplication` class:

    ```kotlin
    @Bean
    fun paymentService() = PaymentService()

    @Bean
    fun hitCounterService() = MemoryHitCounterService()

    @Bean
    fun crashService() = CrashService()
    ```

## Unit Tests

1. Make a new package `microservice.workshop.kotlinredisdemo.http` in the `src/test/kotlin` tree
1. Create a class in the new package called `PaymentControllerTest`
1. Set the content of `PaymentControllerTest` to the following:

    ```kotlin
    package microservice.workshop.kotlinredisdemo.http

    import org.hamcrest.Matchers.`is`
    import org.junit.Before
    import org.junit.Test
    import org.junit.runner.RunWith
    import org.springframework.beans.factory.annotation.Autowired
    import org.springframework.boot.test.context.SpringBootTest
    import org.springframework.http.HttpStatus
    import org.springframework.http.MediaType
    import org.springframework.test.context.junit4.SpringRunner
    import org.springframework.test.web.servlet.MockMvc
    import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
    import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
    import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup
    import org.springframework.web.context.WebApplicationContext

    @RunWith(SpringRunner::class)
    @SpringBootTest
    class PaymentControllerTest {
        private lateinit var mockMvc: MockMvc

        @Autowired
        private lateinit var webApplicationContext: WebApplicationContext

        @Before
        fun setup() {
            this.mockMvc = webAppContextSetup(webApplicationContext).build()
        }

        @Test
        @Throws(Exception::class)
        fun testWithInterest() {
            mockMvc.perform(get("/resetCount"))
                .andExpect(status().`is`(HttpStatus.OK.value()))

            mockMvc.perform(get("/payment?amount=100000&rate=3.5&years=30"))
                .andExpect(status().`is`(HttpStatus.OK.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.payment", `is`(449.04)))
                .andExpect(jsonPath("$.count", `is`(1)))
        }

        @Test
        @Throws(Exception::class)
        fun testZeroInterest() {
            mockMvc.perform(get("/resetCount"))
                .andExpect(status().`is`(HttpStatus.OK.value()))

            mockMvc.perform(get("/payment?amount=100000&rate=0&years=30"))
                .andExpect(status().`is`(HttpStatus.OK.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.payment", `is`(277.78)))
                .andExpect(jsonPath("$.count", `is`(1)))
        }

        @Test
        @Throws(Exception::class)
        fun testThatHitCounterIncrements() {
            mockMvc.perform(get("/resetCount"))
                .andExpect(status().`is`(HttpStatus.OK.value()))

            mockMvc.perform(get("/payment?amount=100000&rate=3.5&years=30"))
                .andExpect(status().`is`(HttpStatus.OK.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.payment", `is`(449.04)))
                .andExpect(jsonPath("$.count", `is`(1)))

            mockMvc.perform(get("/payment?amount=100000&rate=0&years=30"))
                .andExpect(status().`is`(HttpStatus.OK.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.payment", `is`(277.78)))
                .andExpect(jsonPath("$.count", `is`(2)))
        }
    }
    ```

## Testing

1. Run the unit tests:
    - (Windows Command Prompt) `gradlew clean test`
    - (Windows Powershell) `.\gradlew clean test`
    - (Mac/Linux) `./gradlew clean test`
    - Or your IDE's method of running tests

1. Start the application:
    - (Windows Command Prompt) `gradlew bootRun`
    - (Windows Powershell) `.\gradlew bootRun`
    - (Mac/Linux) `./gradlew bootRun`
    - Or your IDE's method of running the main application class

1. Test Swagger [http://localhost:8080](http://localhost:8080)
1. Test the acuator health endpoint [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
1. Test the acuator info endpoint [http://localhost:8080/actuator/info](http://localhost:8080/actuator/info)

## Deploy to Cloud Foundry

1. Create a file `manifest.yml` in the project root directory. Set it's contents to the following:

    ```yaml
    applications:
    - name: KotlinRedisDemo-1.0
      path: build/libs/kotlin-redis-demo-0.0.1-SNAPSHOT.jar
      random-route: true
      env:
        JBP_CONFIG_OPEN_JDK_JRE: '{jre: {version: 12.+}}'
    ```

    Note that this manifest specifies JRE 12+. This is not required if you are using Java 8 (the Java buildpack default). If you are using Java 11, you can change the environment variable accordingly. 

1. Build the application JAR file:
    - (Windows Command Prompt) `gradlew clean bootJar`
    - (Windows Powershell) `.\gradlew clean bootJar`
    - (Mac/Linux) `./gradlew clean bootJar`
    - Or your IDE's method of running the Gradle build

1. `cf push`

You should now be able to test the app with Swagger at the route created by PCF. One thing to note is that the hit counter will reset everytime you deploy (because it is stored in the app state). Also, when the app is scaled the hit counter will not be shared across instances.

## Exercise the Application

### Inspect the Application With the Apps Manager UI

1. Login to Pivotal Apps Manager at https://run.pivotal.io/
1. Inspect the application...specifically:
   - On the app overview page, you should see the Spring Boot logo
   - On the app overview page you should be able to inspect details of the app health
   - On the logs page you should see the recent logs for the application, and be able to change logging levels
   - On the threads page you should be able to obtain a thread dump
   - On the settings page, there should be a Spring Info section. In that section you should be able to see the text you added to the info actuator by pressing the "View Raw JSON" button
   - On the settings page you should be able to see the environment variable we specified in the manifest file, as well as environment variable supplied by Cloud Foundry

### Inspect the Application With the CLI

Some interesting CLI commands...

- `cf target` shows information about your current connection, also used to change org/space if you have more than one
- `cf apps` will show all apps deployed in your current org/space
- `cf routes` will show all routes in your current org/space
- `cf services` will show all services allocated to your current org/space
- `cf app KotlinRedisDemo-1.0` will show detailed information about the application
- `cf stop KotlinRedisDemo-1.0` will start the app
- `cf start KotlinRedisDemo-1.0` will start the app
- `cf events KotlinRedisDemo-1.0` will sho recent events in the applications (starts, stops, etc.)
- `cf logs KotlinRedisDemo-1.0 --recent` will dump recent logging information to the screen
- `cf logs KotlinRedisDemo-1.0` will tail the application log
- `cf ssh-enabled KotlinRedisDemo-1.0` will report whether it is possible to SSH into the application container
- `cf ssh KotlinRedisDemo-1.0` will SSH into the application container

### Scale the Application

Applications can be scaled in two ways - through the app manager UI, or through the CLI. We will use the CLI.

1. Start the loan-calculator-client web page from this repository
1. Enter the URL to your application (like https://redisdemo-10-shy-zebra.cfapps.io)
1. Press the "Start" button. You should see random traffic being generated to the application. You should also see the all traffic is routed to instance 0
1. Scale the app by entering the command `cf scale KotlinRedisDemo-1.0 -i 2` - this will request two instances of the app running. Eventually you should see traffic being routed to the two app instances. Notice that the hit count is not consistent. Why?
1. If you press the "Crash It!" button on the client page, then one of the app instances will crash. Which one depends on how the request was routed. Cloud Foundry will notice that an instance has crashed and will start a new instance automatically - this may take a few minutes to show
1. You can scale the app back down by entering `cf scale KotlinRedisDemo-1.0 -i 1`

You should have noticed that the hit counter is not consistent among the instances, and that it is reset when an app instance crashes. This will demonstrate the idea of epehemeral containers and that Cloud Foundry is designed for stateless applications. We will store the hit count in an external Redis cache in the next section to correct this issue.

## Configure Redis on Cloud Foundry

### Create a Redis Cache Instance

1. Login to Pivotal Apps Manager at https://run.pivotal.io/
1. Navigate to your org/space
1. Select the "services" tab
1. Press the "Add a Service" button
1. Create a new service...
   - Select "Redis Cloud"
   - Select plan type "30 MB" - the free plan
   - Set the instance name to "xxxredis" where "xxx" are your initials

### Add a Redis Based Hit Counter

1. Create a new class `RedisHitCounterService` in the `microservice.workshop.kotlinredisdemo.service` package
1. Set the contents of `RedisHitCounterService` to the following:

    ```kotlin
    package microservice.workshop.kotlinredisdemo.service

    import org.springframework.data.redis.core.RedisTemplate

    class RedisHitCounterService(private val redisTemplate: RedisTemplate<String, Int>) : HitCounterService {

        override fun incrementCounter() =
            with(redisTemplate) {
                opsForValue().setIfAbsent(REDIS_KEY, DEFAULT_VALUE)
                opsForValue().increment(REDIS_KEY)!!
            }

        override fun resetCount() =
            redisTemplate.opsForValue().set(REDIS_KEY, DEFAULT_VALUE)

        companion object {
            const val REDIS_KEY = "payment-calculator"
            const val DEFAULT_VALUE = 5000
        }
    }
    ```

1. Create a class `CloudConfiguration` in the `microservice.workshop.kotlinredisdemo` package. Add the following:

    ```kotlin
    package microservice.workshop.kotlinredisdemo

    import microservice.workshop.kotlinredisdemo.service.RedisHitCounterService
    import org.springframework.context.annotation.Bean
    import org.springframework.context.annotation.Configuration
    import org.springframework.context.annotation.Profile
    import org.springframework.data.redis.serializer.GenericToStringSerializer
    import org.springframework.data.redis.core.RedisTemplate
    import org.springframework.data.redis.connection.RedisConnectionFactory

    @Configuration
    @Profile("cloud")
    class CloudConfiguration(private val redisFactory: RedisConnectionFactory) {

        @Bean
        fun redisTemplate() =
            with(RedisTemplate<String, Int>()) {
                setConnectionFactory(redisFactory)
                valueSerializer = GenericToStringSerializer(Int::class.java)
                this
            }

        @Bean
        fun hitCounterService() =
            RedisHitCounterService(redisTemplate())
    }
    ```

    This configuration is enabled when the "cloud" profile is enabled only. On Cloud Foundry, the Java build pack enables this profile. When enabled, this configuration will create a Redis connection based on the Redis instance bound to the application.

1. Open `KotlinRedisDemoApplication` in the `microservice.workshop.kotlinredisdemo` package
1. Change the `hitCounterService` method so that it is only active when not on the cloud:

    ```kotlin
    @Bean
    @Profile("!cloud")
    fun hitCounterService() = MemoryHitCounterService()
    ```

1. Modify the file `manifest.yml` in the project root directory. Set it's contents to the following:

    ```yaml
    applications:
    - name: KotlinRedisDemo-1.1
      path: build/libs/kotlin-redis-demo-0.0.1-SNAPSHOT.jar
      random-route: true
      env:
        JBP_CONFIG_OPEN_JDK_JRE: '{jre: {version: 12.+}}'
      services:
      - xxxredis
    ```

    Where "xxxredis" maps to the name of the redis instance you created above. Note that you should also change the name of the application to denote the new version.

1. Delete the file `application-cloud.yml` in `src/main/resources`

1. Build the application:
    - (Windows Command Prompt) `gradlew clean bootJar`
    - (Windows Powershell) `.\gradlew clean bootJar`
    - (Mac/Linux) `./gradlew clean bootJar`
    - Or your IDE's method of running the Gradle build

1. `cf push`

You should now be able to test the app with Swagger at the route created by PCF. The hit counter will now persist across deploymant and will be consistent as the app scales.

## Blue Green Deployments

1. Run `cf target` to determine your space name.
1. `cf create-route <<your_space_name>> cfapps.io --hostname xxx-payment-calculator`
1. `cf map-route KotlinRedisDemo-1.0 cfapps.io --hostname xxx-payment-calculator`

Change the URL in the client page to the new route you've created (https://jgb-payment-calculator.cfapps.io). You should see traffic being routed to your app.

Now route traffic to the 1.1 version of the app:

- `cf map-route KotlinRedisDemo-1.1 cfapps.io --hostname xxx-payment-calculator`

You should now see traffic being routed to the 1.0 and the 1.1 version of the application.

Now take away the route to the 1.0 version of the app:

- `cf unmap-route KotlinRedisDemo-1.0 cfapps.io --hostname xxx-payment-calculator`
