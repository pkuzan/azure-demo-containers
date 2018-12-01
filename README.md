<p align="center">
  <img src="img/azure.png" width="200px"/>
  <br>
  <img src="img/spring-boot-logo.png" width="200px"/>
</p>

# Deploy a Spring Boot application to Azure App Service for Containers.


The `Pricer` application is a vanilla Spring Boot application that uses embedded Tomcat and builds to an 
executable jar file containing all dependencies. 

* Spring Data JPA for persistence
  * H2, MySQL, SQL Server
* Angular SPA UI
* RESTful endpoint
  * Spring Rest
* Swagger UI

## Azure App Service  

* Multiple Languages and Frameworks (Linux)
  * Java SE, Tomcat and WildFly
  * .NET Core
  * Ruby
  * Node.js
  * PHP
  * Python
  * Go
  * Docker
* Security and Compliance
  * ISO, SOC, PCI
  * Authenticate with Active Directory
* Global Scale and High Availability
  * Scale out and up automatically 

[For more information, click here](https://docs.microsoft.com/en-gb/azure/app-service/app-service-web-overview)

There are 2 classes of Azure PaaS, App Service and App Service Environments (ASE)

### ASE 
* Dedicated, isolated environments
  * Windows
  * Linux
  * Docker
  * Functions
* Very high scale
* Isolation and secure network access
  * VNET support is essential for on-prem, Express Route connectivity
   
![](img/networkase-overflow.png?raw=true)   

[For more information, click here](https://docs.microsoft.com/en-us/azure/app-service/environment/intro)
## Modifications 
### Code
As App Service supports Java directly (no container required), a Spring Boot executable jar can be deployed with 
no code modifications.
### Configuration
The simplest way to deploy a Java application to App Service is to use the `azure-webapp-maven-plugin`.  
The following plugin needs to be added to `pom.xml`.
```xml
 <plugin>
    <groupId>com.microsoft.azure</groupId>
       <artifactId>azure-webapp-maven-plugin</artifactId>
       <version>1.4.1</version>
       <configuration>
          <deploymentType>jar</deploymentType>

          <!-- configure app to run on port 80, required by App Service -->
          <appSettings>
             <property>
                <name>JAVA_OPTS</name>
                <value>-Dserver.port=80</value>
             </property>
          </appSettings>

          <!-- Web App information -->
          <resourceGroup>userp</resourceGroup>
          <appName>aj-azure-container-demo</appName>
          <region>eastus</region>
          <containerSettings>
             <imageName>${docker.image.prefix}/${project.artifactId}</imageName>
           </containerSettings>
       </configuration>
</plugin>
```
|Parameter | Value |
| --- | --- |
| deploymentType | jar or war |
| resourceGroup | Azure Resource Group to deploy to |
| appName | name of the application - must be unique |
| region | Azure region to deploy to |
| linuxRuntime | Runtime to use |

[For more information, click here](https://github.com/Microsoft/azure-maven-plugins/tree/develop/azure-webapp-maven-plugin)

The Spotify `dockerfile-maven-plugin` can be used to build and push a Docker image to Azure Continer Registry. 
This is added to `pom.xml`.
```xml
<plugin>
   <groupId>com.spotify</groupId>
   <artifactId>dockerfile-maven-plugin</artifactId>
   <version>1.4.0</version>
   <configuration>
      <repository>${docker.image.prefix}/${project.artifactId}</repository>
      <useMavenSettingsForAuth>true</useMavenSettingsForAuth>
      <buildArgs>
         <JAR_FILE>target/${project.build.finalName}.jar</JAR_FILE>
      </buildArgs>
   </configuration>
</plugin>
```
|Parameter | Value |
| --- | --- |
| repository | The name of the ACR registry |
| useMavenSettingsForAuth | Use password from Maven |
| buildArgs | Passed to Dockerfile to make it generic |


### Dockerfile
A `Dockerfile` will need to be added to the project root.  
More information on Dockerfile can be found [here](https://docs.docker.com/engine/reference/builder/).

```
FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
EXPOSE 8080
```

### Authentication
The Azure maven plugin needs to authenticate with Azure, there are 2 ways to do this, via the CLI or using a Service Principal, 
for this demonstration, we'll authenticate via the CLI.

Instructions to install the Azure CLI can be found [here](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest).  
Open a command or terminal and type `az login`, follow the instructions in the response.
```
To sign in, use a web browser to open the page https://microsoft.com/devicelogin and enter the code XXXXXXXXX to authenticate.
```
You are now authenticated.

### Deploy
The application needs to be built and then deployed. In a Terminal window type the following commands.
```
mvn package
mvn dockerfile:build
mvn dockerfile:push
mvn azure-webapp:deploy
```
On successful deployment, text similar to blow will be displayed.
```
mvn azure-webapp:deploy
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building azure-demo 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- azure-webapp-maven-plugin:1.4.1:deploy (default-cli) @ azure-demo ---
AI: INFO 16-11-2018 13:37, 1: Configuration file has been successfully found as resource
AI: INFO 16-11-2018 13:37, 1: Configuration file has been successfully found as resource
[INFO] Authenticate with Azure CLI 2.0
[INFO] Updating target Web App...
[INFO] Successfully updated Web App.
[INFO] Trying to deploy artifact to pk-007-pricer...
[INFO] Successfully deployed the artifact to https://pk-007-pricer.azurewebsites.net
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

The application will not be immediately available as it will take a couple of minutes to launch the 
underlying resources and for the application to start.

## Database Connectivity
The Pricer application, along with most modern Java applications, uses JPA, Hibernate and Spring Data.
Database configuration is a simple matter of setting properties.  
Property placeholders `${}` are resolved in this case by environment variables.

```
spring.datasource.url=jdbc:mysql://pk-007-mysql.mysql.database.azure.com:3306/pricer?useSSL=true&requireSSL=false&serverTimezone=UTC
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=${DB_DDL_AUTO}
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect 
```

Azure App Service supports environment variables that are set in Application Configuration.


## Setup
Create a registry
```
az login
az acr create --admin-enabled --resource-group HelloCloud --location westus --name hellocloudregistry --sku Basic
```

Get credential for registry 
```
az acr credential show --name hellocloudregistry --query passwords[0]
```

Add the registry and credentials in settings.xml

      <server>
           <id>YOUR_REGISTRY_NAME.azurecr.io</id>
           <username>YOUR_REGISTRY_NAME</username>
           <password>PASSWORD_FROM ABOVE</password>
       </server>
     
The Azure maven plugin needs to authenticate with Azure, a Service Principal is used for this purpose. 
```
az account show --query "{subscriptionId:id, tenantId:tenantId}"
az ad sp create-for-rbac --role="Contributor" --scopes="/subscriptions/YOUR_SUBSCRIPTION_ID;"
```

It will respond :
```
{
  "appId": "APP_ID",
  "displayName": "NAME",
  "name": "http://NAME",
  "password": "PASSWORD",
  "tenant": "TENNANT_ID"
}
```

In your Maven settings.xml add the following, replacing the placeholders with your values, this 
will be used to authenticate the plugin.

```
<servers> 
   <server>
     <id>azure-auth</id>
      <configuration>
         <client>APP_ID</client>
         <tenant>TENNANT_ID</tenant>
         <key>PASSWORD</key>
         <environment>AZURE</environment>
      </configuration>
   </server>
</servers> 
```     
You'll end up with 2 servers defined in setteings.xml, one for the Spotify plugin
the other for the Azure plugin.

## Workflow

* Perform setup as above
* dockerfile:build
    * Build the Docker image
* dockerfile:push
    * Push the image to ACR
* azure-webapp:deploy
    * Deploys the image from ACR to App Service     
