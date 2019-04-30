# Spring Boot and JPA Exercise

## Create the Basic Application

1. Navigate to [https://start.spring.io](https://start.spring.io)
1. Create a Maven project with Java and the latest version of Spring Boot (2.1.1 at the time of writing)
1. Specify group: `microservice.workshop`
1. Specify artifact: `jpa-demo`
1. For dependencies, add the following:
    - Web
    - Actuator
    - JPA
    - H2
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
        name: Person Service

    management:
      endpoint:
        health:
          show-details: always
    ```


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

1. Create a class `SwaggerConfiguration` in the `micoservice.workshop.jpademo` package. Add the following:

    ```java
    package microservice.workshop.jpademo;

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

## Create a Person Repository

1. Create a package `microservice.workshop.jpademo.model`
1. Create a class in the new package called `Person`
1. Set the content of `Person` to the following:

    ```java
    package microservice.workshop.jpademo.model;

    import javax.persistence.Entity;
    import javax.persistence.GeneratedValue;
    import javax.persistence.GenerationType;
    import javax.persistence.Id;

    @Entity
    public class Person {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;
        private String firstName;
        private String lastName;

        // getters/setters
    }
    ```

1. Create a package `microservice.workshop.jpademo.data`
1. Create an interface in the new package called `PersonRepository`
1. Set the content of `PersonRepository` to the following:

    ```java
    package microservice.workshop.jpademo.data;

    import java.util.List;
    import org.springframework.data.jpa.repository.JpaRepository;
    import microservice.workshop.jpademo.model.Person;

    public interface PersonRepository extends JpaRepository<Person, Integer> {
        List<Person> findByLastName(String lastName);    
    }
    ```
1. Create a file called `import.sql` in `src/main/resources`. Set the contents to the following:

    ```sql
    insert into person(first_name, last_name) values('Fred', 'Flintstone');
    insert into person(first_name, last_name) values('Wilma', 'Flintstone');
    insert into person(first_name, last_name) values('Barney', 'Rubble');
    insert into person(first_name, last_name) values('Betty', 'Rubble');
    ```

## Create a REST Controller

1. Create a package `microservice.workshop.jpademo.http`
1. Create a class in the new package called `PersonController`
1. Set the content of `PersonController` to the following:

    ```java
    package microservice.workshop.jpademo.http;

    import java.util.List;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestParam;
    import org.springframework.web.bind.annotation.RestController;
    import microservice.workshop.jpademo.data.PersonRepository;
    import microservice.workshop.jpademo.model.Person;

    @RestController
    @RequestMapping("/person")
    public class PersonController {

        @Autowired
        private PersonRepository personRepository;

        @GetMapping
        public ResponseEntity<List<Person>> findAll() {
            return ResponseEntity.ok(personRepository.findAll());
        }
    
        @GetMapping("/{id}")
        public ResponseEntity<Person> findById(@PathVariable("id") Integer id) {
            return ResponseEntity.of(personRepository.findById(id));
        }

        @GetMapping("/search")
        public ResponseEntity<List<Person>> search(@RequestParam("lastName") String lastName) {
            return ResponseEntity.ok(personRepository.findByLastName(lastName));
        }
    }
    ```

## Unit Tests

1. Make a new package `microservice.workshop.jpademo.http` in the `src/test/java` tree
1. Create a class in the new package called `PersonControllerTest`
1. Set the content of `PersonControllerTest` to the following:

    ```java
    package microservice.workshop.jpademo.http;
    
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
    public class PersonControllerTest {
        private MockMvc mockMvc;
    
        @Autowired
        private WebApplicationContext webApplicationContext;
    
        @Before
        public void setup() {
            this.mockMvc = webAppContextSetup(webApplicationContext).build();
        }
    
        @Test
        public void testFindAll() throws Exception {
            mockMvc.perform(get("/person"))
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$", hasSize(4)));
        }

        @Test
        public void testFindOne() throws Exception {
            mockMvc.perform(get("/person/1"))
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$.firstName", is("Fred")))
            .andExpect(jsonPath("$.lastName", is("Flintstone")));
        }

        @Test
        public void testFindNone() throws Exception {
            mockMvc.perform(get("/person/22"))
            .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        }

        @Test
        public void testSearch() throws Exception {
            mockMvc.perform(get("/person/search?lastName=Rubble"))
            .andExpect(status().is(HttpStatus.OK.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$", hasSize(2)));
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

## Configure for Cloud Foundry

1. Create a new database
    1. Login to Pivotal Apps Manager
    1. Navigate to your org/space
    1. Select the "services" tab
    1. Press the "Add a Service" button
    1. Create a new service...
        - (With the Azure Service Broker)
        - Select "Azure SQL Database Service"
        - Select plan type "Basic"
        - name the instance "xxxsql" where "xxx" are your initials

1. Create and open a file `application-cloud.yml` in `src/main/resources`
1. Add this value

    ```yml
    spring:
      jpa:
        hibernate:
          ddl-auto: create-drop
    ```
    This configuration file will only be active when the "cloud" profile is active. This configuration tells Hibernate to create the database (and execute the import script). Normally, Hibernate will only do this for embedded databases like H2 - so without this configuration, the database will not be initialized.

    **Very Important**: Do not set this value in actual production systems or it will cause your production database to be dropped everytime the application starts. I have learned this the hard way :)

1. Create a file `manifest.yml` in the project root directory. Set it's contents to the following:

    ```yaml
    applications:
    - name: SpringBoot-JPA
      path: target/springboot-demo-0.0.1-SNAPSHOT.jar
      random-route: true
      services:
      - xxxsql
    ```

1. Build the application:
    - (Windows Command Prompt) `mvnw clean package`
    - (Windows Powershell) `.\mvnw  clean package`
    - (Mac/Linux) `./mvnw  clean package`
    - Or your IDE's method of running the Maven build

1. `cf push`

You should now be able to test the app with Swagger at the route created by PCF.
