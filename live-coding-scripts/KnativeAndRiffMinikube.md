# Using Knative with Riff and Minikube

Use the riff CLI for Knative with minikube. Riff is a CLI for Knative that makes the serving and build portions easier. Riff also includes support for Knative eventing, although this is a rapidly evolving space. This also demonstrates building and deploying a function using Spring Boot and the cloud native build packs build template.

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

1. Install riff

    ```bash
    brew install riff
    ```

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
    riff system install --node-port
    ```

    Wait until all the pods have initialized

## Install and Run an Application from a Docker Image

Source: https://projectriff.io/docs/getting-started/minikube/

1. Install the hello world app

    ```bash
    riff service create hello --image gcr.io/knative-samples/helloworld-go --env TARGET='Go Sample V1'
    ```

1. Invoke the Service

    ```bash
    riff service invoke hello
    ```

## Build a JavaScript Function from Source

1. Create a file `square.js` and set its contents to the following:

    ```javascript
    module.exports = x => {
        const xx = x ** 2;
        console.log(`the square of ${x} is ${xx}`);
        return xx;
    }
    ```
1. Push the code to GitHub (or just use the code at https://github.com/projectriff-samples/node-square)

1. Initialize the namespace for riff (this will prompt for password)

    ```bash
    riff namespace init default --docker-hub [userid]
    ```

1. Create and deploy the function

    ```bash
    riff function create square \
    --git-repo https://github.com/projectriff-samples/node-square \
    --artifact square.js \
    --verbose
    ```

1. Invoke the function

    ```bash
    riff service invoke square --json -- -w '\n' -d 8
    ```

## Build a Java Function from Source

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

1. If you wish, you can test the function by starting it locally, then entering the following command:

    ```bash
    curl localhost:8080 -H "Content-Type: application/json" -d '{"years":30, "amount":100000, "rate":3.5}' -w '\n'
    ```

    You should get a return value of "449.04".

### Deploy the Java Function to Knative With a Knative Build

1. Push the source to GitHub (or just use the code at https://github.com/jeffgbutler/spring-boot-function-demo)

1. Create and deploy the function

    ```bash
    riff function create payment-calculator \
    --git-repo https://github.com/jeffgbutler/spring-boot-function-demo \
    --verbose
    ```
1. Invoke the Function

    ```bash
    riff service invoke payment-calculator --json -- -d '{"years":30, "amount":100000, "rate":3.5}' -w '\n'
    ```

### Deploy the Java Function to Knative With a Local Build

You can also do a local build - which will run in Docker on your machine. If you do this, you will need to login to Docker hub first (or GCR if you are using that). See here for details: https://github.com/projectriff/riff/blob/master/Using-Registries.md

1. Login to Docker

    ```bash
    docker login
    ```

1. Build and Deploy the Function

    ```bash
    riff function create payment-calculator \
    --local-path=$PWD
    ```

1. Invoke the Function

    ```bash
    riff service invoke payment-calculator --json -- -d '{"years":30, "amount":100000, "rate":3.5}' -w '\n'
    ```
