# Live Coding Scripts

This folder contains live coding scripts that can be used to demonstrate various aspects of Pivotal Cloud Foundry and Kubernetes. These scripts can also be used as instructions for developer workshops.

## Cloud Foundry

### Java/Spring Boot

Script | Description
--|--
[Java and JPA](JavaJPA.MD)| Simple JPA app. Uses H2 in-memory database locally, binds to an RDB in Cloud Foundry 
[Java and Redis](JavaRedis.MD) | Simple Redis app. Uses in-memory counter when run locally, binds to Redis in Cloud Foundry

### C#/.Net Core / Steeltoe

Script | Description
--|--
[C# and Redis](DotNetCoreRedis.md) | Simple Redis app. Uses in-memory counter when run locally, binds to Redis in Cloud Foundry

## Kubernetes

### Knative

Script | Description
--|--
[Raw Knative Serving with Minikube](KnativeMinikube.md) | Simple Knative install and serving with raw Knative / Kubectl commands
[Knative Serving with Knctl and Minikube](KnativeAndKnctlMinikube.md) | Simple Knative install and serving using Knctl
[Knative Serving and Build with riff and Minikube](KnativeAndRiffMinikube.md) | Simple Knative install, serving, and Build using riff
