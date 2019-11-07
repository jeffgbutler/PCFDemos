# Spring Boot, Docker, and Kubernetes Exercise

## Pre-Requisites

### Install an IDE

Install and configure a Java IDE you are comfortable with. Good options include:

- Eclipse: https://www.eclipse.org/
- IntelliJ: https://www.jetbrains.com/idea/
- Visual Studio Code: https://visualstudio.microsoft.com/

If you install Visual Studio Code, then add the following extensions:

- (Microsoft) Java Extension Pack
- (Pivotal) Spring Boot Extension Pack

### Install Docker Desktop

Install Docker desktop from https://www.docker.com/products/docker-desktop

### Install the Kubernetes CLI

Install the Kubernetes CLI (Kubectl) following instructions here: https://kubernetes.io/docs/tasks/tools/install-kubectl/

### Install Minikube (Optional)

This step is optional if you plan on using a Kubernetes cluster other than minikube.

Follow the instructions for installing minikube here: https://kubernetes.io/docs/tasks/tools/install-minikube/

## Create the Basic Application

1. Navigate to [https://start.spring.io](https://start.spring.io)
1. Create a Maven project with Java and the latest version of Spring Boot (2.1.5 at the time of writing)
1. Specify group: `microservice.workshop`
1. Specify artifact: `k8s-demo`
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
    - VS Code: File->Add Folder to Workspace (or just open the folder by navigating to it and entering the command `code .`)

## Initial Configuration

1. Rename `application.properties` in `src/main/resources` to `application.yml`
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

    This sets an application name for the info actuator, and enables more detail in the health actuator.

1. Create a file called `application-default.yml` in `src/main/resources`
1. Set its content to the following:

    ```yml
    spring:
      autoconfigure:
        exclude:
          - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
    ```

    This will tell Springboot not to configure Redis when we're running locally - even though Redis is on the classpath. Failure to do this will not stop the application from starting and running successfully. But the health actuator will show the application being down. 

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

1. Create a class `SwaggerConfiguration` in the `micoservice.workshop.k8sdemo` package. Add the following:

    ```java
    package microservice.workshop.k8sdemo;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.web.bind.annotation.RestController;
    import springfox.documentation.builders.RequestHandlerSelectors;
    import springfox.documentation.spi.DocumentationType;
    import springfox.documentation.spring.web.plugins.Docket;
    import springfox.documentation.swagger2.annotations.EnableSwagger2;

    @Configuration
    @EnableSwagger2
    public class SwaggerConfiguration {
        @Bean
        public Docket api() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .select()
                    .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                    .build();
        }
    }
    ```

    This configuration does two important things:

    1. It enables Swagger
    1. It tells Springfox that we only want to use Swagger for REST controllers. Without this there will be Swagger documentation for the redirect controller, as well as the basic Spring error controller and we usually don't want this.

## Create a Payment Service

1. Create a package `microservice.workshop.k8sdemo.service`
1. Create a class in the new package called `PaymentService`
1. Set the content of `PaymentService` to the following:

    ```java
    package microservice.workshop.k8sdemo.service;

    import java.math.BigDecimal;
    import java.math.RoundingMode;

    import org.springframework.stereotype.Service;

    @Service
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
            double payment = (monthlyRate * amount) /
                (1.0 - Math.pow(1.0 + monthlyRate, -numberOfPayments));
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

1. Create an interface in the `microservice.workshop.k8sdemo.service` package called `HitCounterService`
1. Set the content of `HitCounterService` to the following:

    ```java
    package microservice.workshop.k8sdemo.service;

    public interface HitCounterService {
        long incrementCounter();
        void resetCount();
    }
    ```

1. Create a class in the `microservice.workshop.k8sdemo.service` package called `MemoryHitCounterService`
1. Set the content of `MemoryHitCounterService` to the following:

    ```java
    package microservice.workshop.k8sdemo.service;

    import org.springframework.stereotype.Service;

    @Service
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

## Create a Crash Service

1. Create a class in the `microservice.workshop.k8sdemo.service` package called `CrashService`
1. Set the content of `CrashService` to the following:

    ```java
    package microservice.workshop.k8sdemo.service;

    import java.util.concurrent.Executors;
    import java.util.concurrent.ScheduledExecutorService;
    import java.util.concurrent.TimeUnit;

    import org.springframework.stereotype.Service;

    @Service
    public class CrashService {
        private ScheduledExecutorService executer = Executors.newScheduledThreadPool(1);
    
        // calls System.exit after a 2 second delay
        public void crashIt() {
            executer.schedule(() -> System.exit(22), 2000, TimeUnit.MILLISECONDS);
        }
    }
    ```

## Create a Return Model

1. Create a package `microservice.workshop.k8sdemo.model`
1. Create a class in the new package called `CalculatedPayment`
1. Set the content of `CalculatedPayment` to the following:

    ```java
    package microservice.workshop.k8sdemo.model;

    import java.math.BigDecimal;

    public class CalculatedPayment {
        private double amount;
        private double rate;
        private int years;
        private BigDecimal payment;
        private String instance;
        private Long count;
    
        // TODO: add getters and setters for all fields...
    }
    ```

## Create a REST Controller for the Payment Service

1. Create a package `microservice.workshop.k8sdemo.http`
1. Create a class in the new package called `PaymentController`
1. Set the content of `PaymentController` to the following:

    ```java
    package microservice.workshop.k8sdemo.http;

    import java.math.BigDecimal;

    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestParam;
    import org.springframework.web.bind.annotation.RestController;

    import microservice.workshop.k8sdemo.model.CalculatedPayment;
    import microservice.workshop.k8sdemo.service.HitCounterService;
    import microservice.workshop.k8sdemo.service.PaymentService;

    @RestController
    @RequestMapping("/payment")
    public class PaymentController {

        @Value("${MY_POD_NAME:local}")
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
            calculatedPayment.setInstance(instance);
            calculatedPayment.setCount(hitCounterService.incrementCounter());
        
            return calculatedPayment;
        }
    }
    ```
## Create a REST Controller to Reset the Hit Count

This is needed for the unit tests - it will reset the hit counter to a known state for each test.

1. Create a class `ResetHitCounterController` in package `microservice.workshop.k8sdemo.http`
1. Set the content of `ResetHitCounterController` to the following:

    ```java
    package microservice.workshop.k8sdemo.http;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;

    import microservice.workshop.k8sdemo.service.HitCounterService;

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

## Create a REST Controller to Crash the Application

This is needed to demonstrate Kubernetes' self-healing capabilities.

1. Create a class `CrashController` in package `microservice.workshop.k8sdemo.http`
1. Set the content of `CrashController` to the following:

    ```java
    package microservice.workshop.k8sdemo.http;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;

    import io.swagger.annotations.ApiOperation;
    import microservice.workshop.k8sdemo.service.CrashService;

    @RestController
    @RequestMapping("/crash")
    public class CrashController {

        @Autowired
        private CrashService crashService;

        @ApiOperation("Warning! The application will crash 2 seconds after this method is called")
        @GetMapping()
        public String crashIt() {
            crashService.crashIt();
            return "OK";
        }
    }
    ```

## Setup SPA Client

This repository includes a vue.js based single page application (SPA) that you can use to exercise the application. Copy the files [app.js](client/app.js) and [index.html](client/index.html) into the `src/main/resources/static` folder.


## Unit Tests

1. Make a new package `microservice.workshop.k8sdemo.http` in the `src/test/java` tree
1. Create a class in the new package called `PaymentControllerTest`
1. Set the content of `PaymentControllerTest` to the following:

    ```java
    package microservice.workshop.k8sdemo.http;

    import static org.hamcrest.Matchers.*;
    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
    import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Test;
    import org.junit.jupiter.api.extension.ExtendWith;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.test.context.SpringBootTest;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.MediaType;
    import org.springframework.test.context.junit.jupiter.SpringExtension;
    import org.springframework.test.web.servlet.MockMvc;
    import org.springframework.web.context.WebApplicationContext;

    @ExtendWith(SpringExtension.class)
    @SpringBootTest
    public class PaymentControllerTest {
        private MockMvc mockMvc;
    
        @Autowired
        private WebApplicationContext webApplicationContext;

        @BeforeEach
        public void setup() {
            this.mockMvc = webAppContextSetup(webApplicationContext).build();
        }

        @Test
        public void testWithInterest() throws Exception {
            mockMvc.perform(get("/resetCount"))
            .andExpect(status().is(HttpStatus.OK.value()));
            
            mockMvc.perform(get("/payment?amount=100000&rate=3.5&years=30"))
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.payment", is(449.04)))
            .andExpect(jsonPath("$.count", is(1)));
        }

        @Test
        public void testZeroInterest() throws Exception {
            mockMvc.perform(get("/resetCount"))
            .andExpect(status().is(HttpStatus.OK.value()));
            
            mockMvc.perform(get("/payment?amount=100000&rate=0&years=30"))
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.payment", is(277.78)))
            .andExpect(jsonPath("$.count", is(1)));
        }

        @Test
        public void testThatHitCounterIncrements() throws Exception {
            mockMvc.perform(get("/resetCount"))
            .andExpect(status().is(HttpStatus.OK.value()));
            
            mockMvc.perform(get("/payment?amount=100000&rate=3.5&years=30"))
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.payment", is(449.04)))
            .andExpect(jsonPath("$.count", is(1)));

            mockMvc.perform(get("/payment?amount=100000&rate=0&years=30"))
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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


## Containerize the Application

We now have an executable JAR file, this is easy to put into a container. We'll show several ways to do this.

### Build Container Manually

1. Create a file called `Dockerfile` in the application root directory, set the contents to the following:

    ```Dockerfile
    FROM openjdk:8-jdk-alpine
    COPY target/*.jar app.jar
    EXPOSE 8080
    ENTRYPOINT ["java","-jar","/app.jar"]
    ```

1. Execute the following commands to build the container:

    ```bash
    ./mvnw clean package
    docker build -t jeffgbutler/payment-service:1.0 .
    ```

1. Execute the following command to push the image to Docker hub:

    ```bash
    docker login
    docker push jeffgbutler/payment-service:1.0
    ```

1. Execute the following command to execute the container:

    ```bash
    docker run -d -p 8080:8080 jeffgbutler/payment-service:1.0
    ```
    
    This starts the container in "detached" mode - meaning it runs as a background process. This also exposes port 8080 inside the container as port 8080 on localhost. So we can now access the app exactly as we did before.

You can start additional instances of the container by specifying a different external port. For example:

```bash
docker run -d -p 8081:8080 jeffgbutler/payment-service:1.0
docker run -d -p 8082:8080 jeffgbutler/payment-service:1.0
```

Interesting docker commands:

- `docker ps` or `docker container ls` - list running containers (you can get the ID of the running container here for use in other commands)
- `docker ps -a` or `docker container ls -a` - list all containers (stopped, killed, etc.)
- `docker inspect <container_id>` or `docker container inspect <container_id>` - show detailed information about the container
- `docker logs <container_id>` or `docker container logs <container_id>` - fetch logs from a container
- `docker stop <container_id>` or `docker container stop <container_id>` - stop a container
- `docker start <container_id>`  or `docker container start <container_id>` -start a stopped container
- `docker container prune` remove all stopped containers


### Use the Jib Maven Plugin

TODO

### Use Pivotal Build Service

TODO

## Attach to Redis for Persistent State

We have seen that the memory based hit counter is an issue - when new instances are started the count resets to zero. This is an example of a problem with running a stateful application in a container. We will now make changes to the application to use an external Redis server to store the application state.

### Add a Redis Based Hit Counter

1. Create a new class `RedisHitCounterService` in the `microservice.workshop.k8sdemo.service` package
1. Set the contents of `RedisHitCounterService` to the following:

    ```java
    package microservice.workshop.k8sdemo.service;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.context.annotation.Profile;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.stereotype.Service;

    @Service
    @Profile("cloud")
    public class RedisHitCounterService implements HitCounterService {

        private static final String REDIS_KEY = "payment-calculator";
        private static final int DEFAULT_VALUE = 5000;

        @Autowired
        private RedisTemplate<String, Integer> redisTemplate;

        @Override
        public long incrementCounter() {
            redisTemplate.opsForValue().setIfAbsent(REDIS_KEY, DEFAULT_VALUE);
            return redisTemplate.opsForValue().increment(REDIS_KEY);
        }

        @Override
        public void resetCount() {
            redisTemplate.opsForValue().set(REDIS_KEY, DEFAULT_VALUE);
        }
    }
    ```

1. Create a class `CloudConfiguration` in the `microservice.workshop.k8sdemo` package. Add the following:

    ```java
    package microservice.workshop.k8sdemo;

    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.context.annotation.Profile;
    import org.springframework.data.redis.connection.RedisConnectionFactory;
    import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
    import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.data.redis.serializer.GenericToStringSerializer;

    @Configuration
    @Profile("cloud")
    public class CloudConfiguration {
        @Value("${redisServer:localhost}")
        private String redisServer;

        @Value("${redisServerPort:6379}")
        private int redisServerPort;

        @Bean
        public LettuceConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redisServer, redisServerPort));
        }

        @Bean
        public RedisTemplate<String, Integer> redisTemplate(RedisConnectionFactory redisFactory) {
            RedisTemplate<String, Integer> template = new RedisTemplate<>();
            template.setConnectionFactory(redisFactory);
            template.setValueSerializer(new GenericToStringSerializer<>(Integer.class));
            return template;
        }
    }
    ```

    This configuration is enabled when the "cloud" profile is enabled only. When enabled, this configuration will create a Redis template and connection based on the Redis instance available at the environment variables passed into the application.

1. Open `MemoryHitCountService` in the `microservice.workshop.k8sdemo.service` package
1. Change the service so that it is only available when not on the cloud:

    ```java
    @Service
    @Profile("!cloud")
    public class MemoryHitCounterService implements HitCounterService {
        ...
    }
    ```

1. Start a Redis server in Docker:

    ```bash
    docker run --name my-redis-container -d redis
    ```

1. Kill any existing containers running the old version of the application (`docker ps`, `docker kill <container_id>`)

1. Build a new version of the container:

    ```bash
    ./mvnw clean package
    docker build -t jeffgbutler/payment-service:1.1 .
    ```

    Note that we incremented the version

1. Push the new version to Docker hub:

    ```bash
    docker push jeffgbutler/payment-service:1.1
    ```


1. Run the new version of the container:

    ```bash
    docker run -d -p 8080:8080 --link my-redis-container:redis --env redisServer=my-redis-container \
    jeffgbutler/payment-service:1.1 --spring.profiles.active=cloud
    ```

    This run command links the new container to the Redis container (makes it visible), sets the environment variable for the Redis server so Spring can make the connection, and enables the cloud profile.

    You can now start multiple versions of the container with varients of this command and see that the hit count is consistent.

## Kubernetes

So far we've seen that it is easy to put a Springboot application into a container and run it with Docker. We've also seen that we can easily use published Docker containers to quickly stand up services like Redis and use them in our applications.

But there are also some problems we've seen:

1. If a container dies, it is not restarted
1. To upgrade the application, we kill the old instances and start new instances - causing potential down time
1. We can start multiple instances of a container, but we would need to create a load balancer if we wanted to just have one URL for the multiple instances
1. Linking containers seems a little odd

These issues, and more, are why we need a container orchestrator. There are many container orchestrators on the market:

- Diego inside Pivotal Cloud Foundry
- Docker Swarm
- Mesosphere
- Kubernetes

Kubernetes is the clear winner at this time and it is causing a major disruption in the industry.

### Declarative Versus Imperative

In Kubernetes there is often more than one way to do something. Usually these alternative methods are classifies as "declarative" or "imperative". "Declarative" means that we "declare" our desired state and ask Kubernetes to make it happen. Usually this meants that we create a YAML configuration file, and tell Kubernetes to change the state of the cluster based on the contents of the YAML file.

"Imperative" means that we tell Kubernetes what to do and in what order to do it.

I'll show examples of both methods below.

### Kubernetes Objects

Kubernetes is configured by creting "objects" of many different types. Kubernetes supplies a variety of objects in the out-of-the-box configuration, but Kubernetes can also be extended with user (or vendor) defined objects. I believe that this ability to extend the basic functions of Kubernetes is one of the leading contributors to its overwhelming success in the market.

In this workshop, we will work with four objects that are fundamental to all Kubernetes applications:

- Pods
- ReplicaSets
- Deployments
- Services

But this is just scratching the surface of Kubernetes.

### Kubectl - the Kubernetes Command Line Interface

All interaction with Kubernetes is performed using Kubectl - the Kubernetes CLI (command line interface). Kubectl must be configured to connect to the particular Kubernetes cluster you are interested in. This configuration is performed in a variety of ways:

- Starting minikube automatically configures Kubectl to talk to the minikube
- The PKS CLI can be used to configure Kubectl to talk to a PKS cluster
- Many others

Before proceding with the workshop, make sure that Kubectl is configured properly for your environment. You can test this by entering the command `kubectl cluster-info` - this should show some details about the cluster you are connected to.

### Pods

In Kubernetes, containers run in "pods". This abstraction is offered to allow multiple containers to be deployed in a single pod. But don't think of this as building a pod to contain an entire application. Rather, think of multiple containers as the ability to add helpers to a microservice (like log forwarding, metrics agents, etc.) 

To create a Pod for the payment service in the imperative way, enter the following command:

```bash
kubectl run payment-service --image=jeffgbutler/payment-service:1.0 --restart=Never
```

To create a Pod for the payment service in the declarative way, create a file called `pod.yml` and set the contents to the following:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: payment-service
spec:
  restartPolicy: Never
  containers:
  - name: payment-service
    image: jeffgbutler/payment-service:1.0
```

Then run the following command:

```bash
kubectl create -f pod.yml
```

Note that the default for `restartPolicy` is `Always` - we're setting it to `Never` to show an equivalent setup to the imperitive method.

Verify the pod is running with the command:

```bash
kubectl get pods
```

By itself, a Pod is not very interesting. There is no way to get traffic onto the container from outside the cluster - which is not very useful for us. Pods are the most fundamental object in Kubernetes and nothing runs without a Pod, but we need higher level abstractions.

Handy command to run commands inside a cluster:

```bash
kubectl run -it --rm --restart=Never alpine --image alpine sh
```

We can then crash the pod with a command like this:

```bash
wget 10.200.64.7:8080/crash
```

(Use the command `kubectl get pod payment-service -o wide` to get the IP address)

### Replica Sets

A replica set is a group of pods running the same containers. We declare to Kubernetes how many replicas we want running and Kubernetes will ensure that the number of pods is running. If a pod dies, Kubernetes will start a new one.

There is no method to create a replica set with the imperative method.

To create a replica set through the declarative method, create a file called `replicaset.yml` and set its contents to the following:

```yml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: payment-replicaset
  labels:
    app: paymentApp
spec:
  template:
    metadata:
      name: payment-pod
      labels:
        app: paymentApp
    spec:
      containers:
      - name: payment-service
        image: jeffgbutler/payment-service:1.0
  replicas: 3
  selector:
    matchLabels:
      app: paymentApp
```

Then run the following command:

```bash
kubectl create -f replicaset.yml
```

There's a lot going on here, but the important thing is Kubernetes will now ensure that 3 pods are running.

Replica sets are rarely used by themselves. More common is to use a Deployment.

### Deployments

Deployments are like replica sets with one added benefit - they know how to do updates and rollbacks. For this reason, it is very common to use Deployments rather than replica sets.

To create a deployment in the imperative manner, use the following command:

```bash
kubectl run payment-service --image=jeffgbutler/payment-service:1.0 --replicas=3
```

To create a deployment with the declarative method, create a file called `deployment.yml` then set its contents to the following:

```yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  labels:
    run: payment-service
  name: payment-service
spec:
  replicas: 3
  selector:
    matchLabels:
      run: payment-service
  strategy:
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
    type: RollingUpdate
  template:
    metadata:
      labels:
        run: payment-service
    spec:
      containers:
      - image: jeffgbutler/payment-service:1.0
        name: payment-service
        imagePullPolicy: Always
        env:
        - name: MY_NODE_NAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
        - name: MY_POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
```

Then run the following command:

```bash
kubectl create -f deployment.yml
```

### Services

Services are how we allow traffic into a Pod.

To create a service in the imperative manner, use the following command:

```bash
kubectl expose deployment payment-service --type=NodePort --target-port=8080 --port=8080
```

To create a service in the declarative manner, create a file called `service.yml` then set its contents to the following:

```yaml
apiVersion: v1
kind: Service
metadata:
  labels:
    run: payment-service
  name: payment-service
spec:
  ports:
  - port: 8080
    targetPort: 8080
  selector:
    run: payment-service
  sessionAffinity: None
  type: NodePort
```

Then run the following command:

```bash
kubectl create -f service.yml
```

You can now access the service at any of the node IP addresses, with the port generated by the service.

Get the IP address for one of the nodes:

```bash
kubectl get node -o wide
```

Get the port for the service (itwill be in the range 30000-32767):

```bash
kubectl get service
```

Access the app - in my case it was http://192.168.133.7:31407

### Stand Up Redis in Kubernetes

```bash
kubectl run redis --image=redis
kubectl expose deployment redis --type=ClusterIP --port=6379 --target-port=6379
```

This creates a single Pod deployment of Redis, and a ClusterIP based service. It also adds the service name ("redis") to the cluster DNS.

### Modify the Deployment

In the `deployment.yml` file, we need to do two things:

1. Turn on the "cloud" profile
1. Set the name of the redis server

The modified file is as follows:

```yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  labels:
    run: payment-service
  name: payment-service
spec:
  replicas: 3
  selector:
    matchLabels:
      run: payment-service
  strategy:
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
    type: RollingUpdate
  template:
    metadata:
      labels:
        run: payment-service
    spec:
      containers:
      - image: jeffgbutler/payment-service:1.1
        name: payment-service
        args:
          - "--spring.profiles.active=cloud"
        imagePullPolicy: Always
        env:
        - name: redisServer
          value: redis
        - name: MY_NODE_NAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
        - name: MY_POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
```

Update the deployment with the following command:

```bash
kubectl apply -f deployment.yml
```

Wait for all the pods to recycle, then verify that the app is now hitting the Redis server.
