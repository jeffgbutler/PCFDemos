# TAS Basics with .NET Framework and Redis
This exercise will cover the following:

- Building a simple web service with ASP.Net (.NET Framework)
- Deploying the service to TAS
- Using Steeltoe to bind to a redis cloud instance
- Doing a blue/green deployment with TAS

## Pre-Requisites

### Visual Studio

1. This demo requires a Windows workstation 
1. Install either Visual Studio IDE from this URL: https://visualstudio.microsoft.com/

### Cloud Foundry Command Line Interface (CLI)

1. Install the Cloud Foundry CLI from this URL: https://docs.cloudfoundry.org/cf-cli/install-go-cli.html
1. Verify the install by opening a terminal or command window and typing `cf --version`. You should see a version string to match the version you installed

### Obtain TAS Credentials

If you are using a private installation of TAS, then obtain credentials and API enpoint information from your TAS platform team. If you are using Pivotal Web Services (the public PCF instance hosted by Pivotal), then go to [https://run.pivotal.io/](https://run.pivotal.io/) and register for a free account.

Once you have credentials, login with the CLI...

1. Open a terminal or command window and login to PCF with the command `cf login -a api.run.pivotal.io` (or whatever API endpoint you are using if not Pivotal Web Services)
1. Enter the email you registered and the password you set

## Build a Simple Web Service

### Create the Basic Project

1. Open VisualStudio
1. Take the option to "Create a New Project"
1. Select the template "ASP.NET Web Application (.NET Framework)
1. Name the project "MicroserviceWorkshop", press "Next"
1. Select the "Web API" template, press "Create"
1. Run the new web service by pressing F5 in Visual Studio, a browser window should open with a default home page
1. Navigate to {base URL}/api/values - you should see an XML document with "value1" and "value2"
1. Stop the service with `Shift-F5` (or press the stop button in Visual Studio)

## Deploy to Tanzu Application Service

1. Create a file `manifest.yml` in the project root directory (not the solution root directory - the project is typically a subdirectory of the solution). Set its contents to the following:

    ```yaml
    applications:
    - name: MicroserviceWorkshop
      random-route: true
      stack: windows
      buildpacks:
      - hwc_buildpack
    ```
1. Open developer powershell and make sure you are in the project root directory
1. `cf push`

During the push process, TAS will create a route for the app. Make note of the route - you can access the application at this URL once the application has started.

Once the application has started, you can access the application at the route created in TAS (something like https://microserviceworkshop-grumpy-elephant.cfapps.io)

