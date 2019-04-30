# Various Demos for PCF

This repo has a variety of demo applications that show different aspects of application development for PCF.

## Loan Calculator Demo

This demo shows how to build, deploy, and scale a simple microservice. The demo includes two version of the microservice in Java using Spring Boot, two version in C# using Steeltoe, and a common client written in react.js. The common client acts as a traffic simulator and the different versions of the service can be used to demonstrate a blue-green deployment.

| Demo | Comments|
|------|---------|
|[Common Client](loan-calculator-client) | React.js client application and UI for all other loan calculator demos |
|[Java Version 1](loan-calculator) | Java microservice using Spring Boot. Has a bug - it has state in memory |
|[Java Version 2](loan-calculator-redis) | Java microservice using Spring Boot. Uses Spring cloud connectors to attach to a Redis service in PCF for storing state |
|[C# Version 1](NetLoanCalculator) | C# microservice using .Net core. Has a bug - it has state in memory |
|[C# Version 2](NetLoanCalculatorRedis) | C# microservice using .Net core. Uses Steeltoe to attach to a Redis service in PCF for storing state |

## Persistence Demos

Demos in this section show various ways of working with relational databases in PCF and Spring Boot. Each demo in this section will attach to a MySql instance when deployed on cloud foundry, or will use an in memory H2 database otherwize.

| Demo | Comments|
|------|---------|
|[Persistence with MyBatis](mybatis-mysql) | Persistence with MyBatis. The UI is swagger based with a simple REST controller |
|[Persistence with JPA](jpa-mysql) | Persistence with JPA/Hibernate. The UI is swagger based with a simple REST controller |
|[Persistence with JPA Version 2](jpa-autorest-mysql) | Persistence with JPA/Hibernate. The REST controller here is created automagically using Spring data rest. Swagger will not generate a UI for Spring data rest at this time |

## Live Coding

Several live coding scripts demonstrating Java, C#, and Knative topics are available in the [live-coding-scripts](live-coding-scripts)
