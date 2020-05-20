# BBData API

This repository is the cornerstone of BBData. It contains: 

1. a SpringBoot Application exposing a REST API for submitting measures, administering objects, users, etc.
2. dockerfiles and docker-compose for local testing, including: MySQL, Cassandra, Kafka
3. the definition of the two databases at the center of BBData: MySQL & Cassandra

## Development setup

### Prerequisites

* Java 1.8+
* IntelliJ IDE with Kotlin support
* Docker

### Setup

Open the project in IntelliJ and let it work. Once finished, you should be able to simply run the app by 
launching the main class `ch.derlin.bbdata.BBDataApplication` (open it and right-click > run).

Of course, you will need MySQL, Cassandra and Kafka running for the whole API to run (to skip some of those deps, 
the the Profiles section).

### Cassandra, MySQL and Kafka

To setup the three dependant services, have a look at the `other` folder.
It contains all the files needed for a production setup, as well as instruction on how to run a Docker container
for local dev/testing.

### Profiles

By default, the app will launch with everything turned on, and will try to connect to MySQL, Cassandra and Kafka on localhost
on default ports (see `src/main/resources/application.properties`).

Profiles let you disable some parts of the application. This is very useful for quick testing.
To enable specific profiles, use the `-Dspring.profiles.active=XX[,YY]` JVM argument.
On IntelliJ: _Edit Configurations ... > VM Options_.


Currently available profiles (see the class `ch.derlin.bbdata.api.Profiles`):

* `unsecured`: all endpoints will be available without apikeys; the userId is automatically set to `1`;
* `input`: will only register the "input" endpoint (`POST /objects/values`);
* `output`: will only register the "output" endpoints (everything BUT the one above);
* `noc`: short for "_No Cassandra_". It won't register endpoints needing a Cassandra connection (input and values);

Profiles can be combined (when it makes sense).

__Examples__:

Output only:
```bash
java -jar bbdata.jar -Dspring.profiles.active=output
```

Output only, no security checks:
```bash
java -jar bbdata.jar -Dspring.profiles.active=output,unsecured
```

Output only, no security checks and no cassandra
(note that the output profile is not needed, as no Cassandra means no input):
```bash
java -jar bbdata.jar -Dspring.profiles.active=noc,unsecured
```

## Production

To deploy in production, you need to build the jar and override some properties in order to connect to the correct services.

1. to build the jar: `./gradlew bootJar`, the jar will be created as `./build/libs/*.jar`
2. to specify properties from a file outside the jar: https://www.baeldung.com/spring-properties-file-outside-jar

### Minimal properties to provide

Here is a sample file, values to change are in UPPERCASE:
```properties
## MySQL properties
spring.datasource.url=jdbc:mysql://HOST:PORT/bbdata2?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&allowPublicKeyRetrieval=true
spring.datasource.username=bbdata-admin
spring.datasource.password=PASSWORD

## Cassandra properties
spring.data.cassandra.contact-points=IP_1,IP_2,IP_X
spring.data.cassandra.consistency-level=quorum

## Kafka properties
spring.kafka.producer.bootstrap-servers=HOST:PORT
spring.kafka.template.default-topic=bbdata2-augmented
```