**After creating the certificates following [this](CREATE_CERTIFICATES.md) tutorial, do as follows.**

Here's how you run our project:

## In the **Client** machine, set your path to .../a06-miguel-guilherme-guilherme.

Run `mvn clean install` and then `mvn exec:java -Dmainclass=pt.ulisboa.tecnico.meditrack.clientapp.Client`.

## In the **API Server** machine, set your path to .../a06-miguel-guilherme-guilherme/App-API-DB/meditrack

Change the `application.properties` file in the `src/main/resources` directory. It should look like this:

```
server.port=8081
server.ssl.key-store=classpath:keystore.jks
server.ssl.key-store-password=your_password
server.ssl.key-password=your_password

spring.datasource.url=jdbc:postgresql://192.168.2.7:8081/meditrack_db
spring.datasource.username=postgres
spring.datasource.password=sysadmin
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update

# When set as true, the database will be reset to default values when the app is launched
app.database-initialization.enabled=true
```

Run `mvn spring-boot:run`.

## In the **DB Server**

You should the database running. Make sure the password for the username `postgres` is `sysadmin`.


