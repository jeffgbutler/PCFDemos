# Using Knative with Minikube

Use the knctl CLI for Knative with minikube. Knctl is a CLI for Knative that makes the serving and build portions easier. Knctl will probably eventually be merged into the new official Knative client (same people working on both). Knctl does not add anything for eventing. THis also demonstrates building and deploying a function using Spring Boot and the Kaniko build template.

## Install Tools (Mac OS)

1. Install minikube

    ```bash
    brew cask install minikube
    ```

1. Install Hyperkit Driver

    ```bash
    brew install docker-machine-driver-hyperkit
    ```

1. Install Kubernetes CLI

    ```bash
    brew install kubernetes-cli
    ```

1. Install watch

    ```bash
    brew install watch
    ```

1. Install Knctl

    - Downlod the latest release from https://github.com/cppforlife/knctl/releases
    - Make it executable: `chmod +x knctl-darwin-amd64`
    - Put it in the path: `mv knctl-darwin-amd64 /usr/local/bin/knctl`
    - Test: `knctl version`

## Create a Minikube Cluster and Install Knative

1. Create and start a minikub cluster
    ```bash
    minikube start --memory=8192 --cpus=4 \
      --vm-driver=hyperkit \
      --disk-size=30g \
      --extra-config=apiserver.enable-admission-plugins="LimitRanger,NamespaceExists,NamespaceLifecycle,ResourceQuota,ServiceAccount,DefaultStorageClass,MutatingAdmissionWebhook"
    ```

1. Start a pod monitor in a separate bash window

    ```bash
    watch -n 1 kubectl get pod --all-namespaces
    ```

1. Install Knative

    ```bash
    knctl install --node-ports --exclude-monitoring
    ```

    Wait until all the pods have initialized

## Install and Run an Application

Source: https://github.com/cppforlife/knctl/blob/master/docs/basic-workflow.md

1. Install the hello world app

    ```bash
    knctl deploy --service hello --image gcr.io/knative-samples/helloworld-go --env TARGET='Go Sample V1'
    ```

1. curl the Service

    ```bash
    knctl curl --service hello
    ```

## Build a Function from Source

### Create the Function

1. Navigate to [https://start.spring.io](https://start.spring.io)
1. Create a Maven project with Java and the latest version of Spring Boot
1. Specify group: `com.example`
1. Specify artifact: `function-demo`
1. For dependencies, add the following:
    - Cloud Function
    - Reactive Web
1. Generate the project (causes a download)
1. Unzip the downloaded file somewhere convenient
1. Add the new project to your IDE workspace
    - Eclipse: File->Import->Existing Maven Project
    - IntelliJ: File->New->Module From Existing Sources...
    - VS Code: File->Add Folder to Workspace

### Create a Payment Calculator

1. Create a class `PaymentCalculator` in the `com.example.functiondemo`
1. Set the contents to the following:

    ```java
    package com.example.functiondemo;

    import java.math.BigDecimal;
    import java.math.RoundingMode;

    public class PaymentCalculator {
        private double amount;
        private double rate;
        private int years;

        public PaymentCalculator(double amount, double rate, int years) {
            this.amount = amount;
            this.rate = rate;
            this.years = years;
        }

        public BigDecimal calculatePayment() {
            if (rate == 0.0) {
                return calculateWithoutInterest();
            } else {
                return calculateWithInterest();
            }
        }

        private BigDecimal calculateWithInterest() {
            double monthlyRate = rate / 100.0 / 12.0;
            int numberOfPayments = years * 12;
            double payment = (monthlyRate * amount) / (1.0 - Math.pow(1.0 + monthlyRate, -numberOfPayments));
            return toMoney(payment);
        }

        private BigDecimal calculateWithoutInterest() {
            int numberOfPayments = years * 12;
            return toMoney(amount / numberOfPayments);
        }

        private BigDecimal toMoney(double d) {
            BigDecimal bd = new BigDecimal(d);
            return bd.setScale(2, RoundingMode.HALF_UP);
        }
    }
    ```

1. Open the `FunctionDemoApplication` class and add the following bean definition"

    ```java
    @Bean
    public Function<PaymentCalculator, BigDecimal> payment() {
        return PaymentCalculator::calculatePayment;
    }
    ```

1. Create a file `Dockerfile` in the root directory of the project.
1. Set the contents of `Dockerfile` to the following:

    ```dockerfile
    FROM openjdk:8-jdk-alpine as build
    WORKDIR /build

    COPY mvnw .
    COPY .mvn .mvn
    COPY pom.xml .
    COPY src src

    RUN ./mvnw install -DskipTests
    RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

    FROM openjdk:8-jdk-alpine
    VOLUME /tmp
    ARG DEPENDENCY=/build/target/dependency
    COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
    COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
    COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
    ENTRYPOINT ["java","-cp","app:app/lib/*","com.example.functiondemo.FunctionDemoApplication"]
    ```

    Note this is a standard Docker file for building a Spring Boot application in a multistage build with one important difference - rather than using the standard WORKDIR of /workspace/app, we use /build. This is due to the way that Kaniko works - it will ignore the /workspace directory between build stages.

1. If you wish, you can test the function by starting it locally, then entering the following command:

    ```bash
    curl localhost:8080 -H "Content-Type: application/json" -d '{"years":30, "amount":100000, "rate":3.5}' -w '\n'
    ```

    You should get a return value of "449.04".

### Deploy the function to Knative

kubectl create ns deploy-from-source

export KNCTL_NAMESPACE=deploy-from-source

knctl basic-auth-secret create -s docker-reg1 --docker-hub -u \[userid\] -p \[password\]

knctl service-account create -a serv-acct1 -s docker-reg1

knctl deploy \
    --service payment-calculator \
    --directory=$PWD \
    --service-account serv-acct1 \
    --image index.docker.io/jeffgbutler/payment-calculator
