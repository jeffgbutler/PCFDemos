# Using Knative with Minikube

Use the knctl CLI for Knative with minikube. Knctl is a CLI for Knative that makes the serving and build portions easier. Knctl will probably eventually be merged into the new official Knative client (same people working on both). Knctl does not add anything for eventing.

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
