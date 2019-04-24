# Using Knative with Minikube

Use the raw Knative constructs with minikube.

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

## Create a Minikube Cluster and Install Knative

Source: https://knative.dev/docs/install/knative-with-minikube/

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

1. Install Istio

    ```bash
    kubectl apply --filename https://github.com/knative/serving/releases/download/v0.5.1/istio-crds.yaml

    curl -L https://github.com/knative/serving/releases/download/v0.5.1/istio.yaml \
      | sed 's/LoadBalancer/NodePort/' \
      | kubectl apply --filename -

    kubectl label namespace default istio-injection=enabled
    ```

    Wait until all the Istio pods have initialized

1. Install Knative CRDs

    Note: this instructions install Knative without the monitoring components which are resource intensive

    ```bash
    kubectl apply --selector knative.dev/crd-install=true \
    --filename https://github.com/knative/serving/releases/download/v0.5.1/serving.yaml \
    --filename https://github.com/knative/build/releases/download/v0.5.0/build.yaml \
    --filename https://github.com/knative/eventing/releases/download/v0.5.0/release.yaml \
    --filename https://github.com/knative/eventing-sources/releases/download/v0.5.0/eventing-sources.yaml \
    --filename https://raw.githubusercontent.com/knative/serving/v0.5.1/third_party/config/build/clusterrole.yaml
    ```

    Note that you may need to run this command twice if there are errors.

1. Install Knative

    ```bash
    kubectl apply \
    --filename https://github.com/knative/serving/releases/download/v0.5.1/serving.yaml \
    --filename https://github.com/knative/build/releases/download/v0.5.0/build.yaml \
    --filename https://github.com/knative/eventing/releases/download/v0.5.0/release.yaml \
    --filename https://github.com/knative/eventing-sources/releases/download/v0.5.0/eventing-sources.yaml \
    --filename https://raw.githubusercontent.com/knative/serving/v0.5.1/third_party/config/build/clusterrole.yaml
    ```

    Wait until all the pods have initialized

## Install and Run an Application

Source: https://knative.dev/docs/install/getting-started-knative-app/

1. Create a file `service.yml` with this content:

    ```yaml
    apiVersion: serving.knative.dev/v1alpha1 # Current version of Knative
    kind: Service
    metadata:
      name: helloworld-go # The name of the app
      namespace: default # The namespace the app will use
    spec:
      runLatest:
        configuration:
          revisionTemplate:
            spec:
              container:
                image: gcr.io/knative-samples/helloworld-go # The URL to the image of the app
                env:
                  - name: TARGET # The environment variable printed out by the sample app
                    value: "Go Sample v1"    ```
    ```

1. Apply the new service

    ```bash
    kubectl apply --filename service.yml
    ```

    Watch the Pods initialize

1. Compute the Istio Ingress IP Address

    ```bash
    export IP_ADDRESS=$(kubectl get node  --output 'jsonpath={.items[0].status.addresses[0].address}'):$(kubectl get svc istio-ingressgateway --namespace istio-system   --output 'jsonpath={.spec.ports[?(@.port==80)].nodePort}')
    ```

1. Compute the Service Host Name

    ```bash
    export HOST_URL=$(kubectl get route helloworld-go  --output jsonpath='{.status.domain}')
    ```

1. curl the Service

    ```bash
    curl -H "Host: ${HOST_URL}" http://${IP_ADDRESS}
    ```
