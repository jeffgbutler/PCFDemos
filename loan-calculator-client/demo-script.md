# Initial Setup

## Create Routes and Service

- cf create-route jgbutler cfapps.io --hostname jgb-loan-calculator
- cf create-route jgbutler cfapps.io --hostname jgb-loan-calculator-10
- cf create-route jgbutler cfapps.io --hostname jgb-loan-calculator-11
- cf create-service rediscloud 30mb jgbredis

### Helpful Commands

- cf map-route loan-calculator-1.0 cfapps.io --hostname jgb-loan-calculator
- cf map-route loan-calculator-1.1 cfapps.io --hostname jgb-loan-calculator
- cf unmap-route loan-calculator-1.0 cfapps.io --hostname jgb-loan-calculator
- cf events loan-calculator-1.0
- cf logs loan-calculator-1.0 --recent

## Startup...

1. Start the react app
2. Deploy the redis version but only have the private URL mapped to it (no need to set through another long cf push)


# Key Concept - Speed

Typically developers want to go fast and make lots of changes where operations wants to keep things stable and running smoothly. Both of these are good goals, but they create a natural tension that ultimately slows innovation. PCF aims to reduce this tension by allowing operations to establish and maintain a solid and controlled infrastructure, but exposing this infrastructure as self-service for developers.

Demo...Get an app deployed to the cloud in minutes

```Shell
  ./mvnw clean package
  cf push
```

Talk a bit about the manifest - show the build pack, route, application name, etc. Mention random routes.

This is a simple Spring Boot app.  There is nothing special about this app for the cloud excpet one thing - it will read and display a PCF related environment variable if it is available. This is important for something I'll show later.

Start the tester with URL `jgb-loan-calculator-10.cfapps.io`

Once we are satisfied that the app works (by testing the private route), map the public route...

```shell
  cf map-route loan-calculator-1.0 cfapps.io --hostname jgb-loan-calculator
```

Stop the tester, change the URL to `jgb-loan-calculator.cfapps.io` and restart.

# Key Concept - Scaling

One of the benefits of a microservice based architecture is that different parts of the application can scale independently. PCF has support for scaling applications out of the box.

Demo...scale the app

```shell
  cf scale loan-calculator-1.0 -i 2
```

It takes a few minutes to scale, but you will see that the app is now running two instances. But you start to see an issue with this app - the hit counter is different for each instance. This is an indicator that the application is holding some state and that is a problem for applications that need to scale. We will need to fix that.

# Key Concept - Resiliency

We often say that PCF is an opinionated platform. Many of the typical issues with going to the cloud are just handled for you - including logging, monitoring, security, etc. One of PCF's opinions is that your app should be running. PCF will automatically monitor your app and will restart it automatically if it crashes.

Demo...crash the app

Notice that the hit counter resets once the second instance starts.

# Key Concept - Services

In PCF, a service can be used to keep application state. Services can be many things - caches, databases, etc. We will use Redis cache to keep our hit count. Talk about how a service instance is sttod up, then what needs to change in the application:

All the tests passed with the in memory hit counter and that is as it should be. We need to add a new instance of the counter service to interact with Redis when we are deployed on the cloud.

- Add dependencies to the POM
- Make a Redis based implementation of the hit counter (we will start the count at 5000 so we can clearly see what's happening)
- Make a CloudConfiguration class that is activated with the "cloud" profile is active
- Change the configuration for the memory based hit counter so that it is only active when not on the cloud

# Key Concept - Blue Green Deployment

Explain a blue green deployment in high level.

Build and deploy the new app (I've already done this, don't need to do it again). We can hit the app on its private URL to test, but let's assume that has been done and it is working.

What we'll do now is map the public URL to the new version. The PCF router will now distribute traffic among the different applications that can serve that route.

```shell
  cf map-route loan-calculator-1.1 cfapps.io --hostname jgb-loan-calculator
```

We should now see hit counts coming from the new version of the app (5000 and above).

We will now unmap the public URL from the old version of the app...

```shell
  cf unmap-route loan-calculator-1.0 cfapps.io --hostname jgb-loan-calculator
```

We should now only see hit counts from the new version. We are now able to scale the app, crash instances, etc. and see a consistent hit count.