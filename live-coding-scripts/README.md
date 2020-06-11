# Live Coding Scripts

This folder contains live coding scripts that can be used to demonstrate various aspects of Pivotal Cloud Foundry and Kubernetes. These scripts can also be used as instructions for developer workshops.

If you have any difficulties with Java on Windows, see this page for some tips: [Java on Windows Tips](JavaInstallOnWindows.md)

## Cloud Foundry

### Java/Spring Boot

Script | Description
--|--
[Java and JPA](JavaJPA.MD)| Simple JPA app. Uses H2 in-memory database locally, binds to an RDB in Cloud Foundry 
[Java and Redis](JavaRedis.MD) | Simple Redis app. Uses in-memory counter when run locally, binds to Redis in Cloud Foundry
[Wavefront](JavaWavefront.MD) | Wavefront Observability with Spring Boot

### Kotlin/Spring Boot

Script | Description
--|--
[Kotlin and Redis](KotlinRedis.MD) | Simple Redis app. Uses in-memory counter when run locally, binds to Redis in Cloud Foundry

### C#/.Net Core / Steeltoe

**Important note:** - .Net Core 2.2 has reached end of life and the corresponding Cloud Foundry buildpack no longer supports version 2.2. Version 2.1 is supported for legacy workloads, but newer versions of Steeltoe do not support .Net Core version 2.1. The bottom line is...use .Net Core 3.1

Script | Description
--|--
[C# and Redis](DotNetCoreRedis.md) | **.Net Core 2.2.X** Simple Redis app. Uses in-memory counter when run locally, binds to Redis in Cloud Foundry
[C# and Redis](DotNetCoreRedis-V3.md) | **.Net Core 3.1.X** Simple Redis app. Uses in-memory counter when run locally, binds to Redis in Cloud Foundry

## Kubernetes

Script | Description
--|--
[Springboot/Docker/Kubernetes](JavaDockerK8s.md) | Springboot running in Docker and Kubernetes 

### Knative

Script | Description
--|--
[Raw Knative with Minikube](KnativeMinikube.md) | Simple Knative install and serving with raw Knative / Kubectl commands
[Knative and Knctl with Minikube](KnativeAndKnctlMinikube.md) | Simple Knative install, serving, and build using Knctl
[Knative and riff with Minikube](KnativeAndRiffMinikube.md) | Simple Knative install, serving, and build using riff
