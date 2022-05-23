[![ebean-test-containers EA](https://github.com/ebean-orm/ebean-test-containers/actions/workflows/jdk-ea.yml/badge.svg)](https://github.com/ebean-orm/ebean-test-containers/actions/workflows/jdk-ea.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.ebean/ebean-test-containers.svg?label=Maven%20Central)](https://mvnrepository.com/artifact/io.ebean/ebean-test-containers)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ebean/ebean-test-containers/blob/master/LICENSE)

# ebean-test-containers
Ability to start, setup and remove docker containers. e.g. Postgres running as docker container for testing.

The issues this solves for us is to:
- Start containers and wait for them to be ready
- Setup containers, e.g. create databases, users, schemas, run scripts as needed
- Cleanup containers on JVM shutdown or explicitly (stop, remove containers)


The needs of this project are primarily driven by the needs/desires of using docker
containers to make testing nice for Ebean ORM, see https://ebean.io/docs/testing/


## Supported Containers

Postgres, ClickHouse, CockroachDB, DB2, ElasticSearch, Hana, LocalDynamoDB, Localstack, MariaDB, MySql, NuoDB, Oracle, Postgres, Redis, SqlServer, Yugabyte.

## Dependency

```xml

  <dependency>
    <groupId>io.ebean</groupId>
    <artifactId>ebean-test-containers</artifactId>
    <version>5.6</version>
    <scope>test</scope>
  </dependency>

```

Note: Was previously `io.ebean:ebean-test-docker` and before that `org.avaje.docker:docker-commands`.


## Designed for fast testing

As developers, we want testing to be fast, we want to be able to run even a single test and for that to be fast.

For a CI environment speed is less of a factor, CI doesn't mind waiting for docker containers to start etc.

ebean-test-docker is designed such that for developers we keep the docker containers running, and even reuse the same
docker container for multiple projects. We do this by:

- Having a unique dbName per project
- Having a ~/.ebean/ignore-docker-shutdown marker file on our development machines (but not in CI environment)

### Unique dbName

The `dbName` should be unique across projects. For example, "my_app1", "my_app2", "my_app3".

This allows us to use the same database container to test multiple applications. We want to do this so that
testing is really fast for developers as we no longer drop/create/start/setup containers for each test run.

The dbName needs to be a valid database name so please just use alpha and underscores and no
special characters. For example, with Postgres it needs to be a valid postgres database name.

```java
    PostgresContainer container = PostgresContainer.newBuilder("14")
      .dbName("my_app1") // this needs to be unique, not clash with other projects
      .build();

    container.start();

```

Some containers like Redis, Localstack, LocalDynamoDB, ElasticSearch, ClickHouse do not have a concept
like "database" and instead multiple projects share a global namespace.

In this way, we either need to make sure our resources (DynamoDB tables, queues, topics etc) are
unique across projects or NOT share containers across projects.


### Marker file - `~/.ebean/ignore-docker-shutdown`

The presence of the marker file tells ebean-test-docker that we are running as a **Developer** and not **CI**.

This means, please don't stop/remove the containers at the end of testing but instead leave the container running.
In this way testing for developers is faster as most frequently ebean-test-dockers only needs to check that the
container is running and that the database is setup and good to go (database/user/schema).


#### For developers running tests

Having the `~/.ebean/ignore-docker-shutdown` marker file tells ebean-test-docker you are a Developer machine
and not CI.

For developers running tests, ebean-test-docker checks if the container is running and only has to start it if
it is not running. It will then create the database/user/schema in that container if necessary. Typically, this
means that to run a test ebean-test-docker only needs to do a quick check that the container is up and the database
is setup and ready for testing.

The `~/.ebean/ignore-docker-shutdown` marker file means that by default it will not stop/remove the container at the
end of testing. It will leave the container there for the next test run.


#### For CI running tests

For CI running tests, it typically needs to start the container, wait for it to be ready, create the database/user/schema.
Then hand off to run all tests. Then on JVM shutdown stop the container and remove the container (cleanup).

In this way, running tests in CI is going to be slower but that is generally expected and OK.

Also note that CI will often also run as Docker-In-Docker and ebean-test-docker handles that case.



## Programmatic use
We can programmatically create the containers. Typically we need to:
1. Create the container
2. Start the container

Typically, we don't need to explicitly stop/remove the container. Instead, the container stop/remove defaults to
occurring automatically on JVM shutdown.

#### Postgres

```java

    PostgresContainer container = PostgresContainer.newBuilder("14")
      //.containerName("ut_postgres")
      //.port(6423)
      .dbName("my_app1")
      .extensions("hstore,pgcrypto")
      .build();

    container.start();

```
... a more extensive example:

```java

    PostgresContainer container = PostgresContainer.newBuilder("14")
      .dbName("my_app2")
      .containerName("temp_postgres14")
      .port(9823)
      .extensions("hstore,pgcrypto")
      .user("main_user")
      .dbName("main_db")
      .initSqlFile("init-main-database.sql")
      .seedSqlFile("seed-main-database.sql")
      // with a second extra database
      .extraDb("extra")
      .extraDbInitSqlFile("init-extra-database.sql")
      .extraDbSeedSqlFile("seed-extra-database.sql")
      .build();

    container.start();
```

#### SqlServer

```java

    SqlServerContainer container = SqlServerContainer.newBuilder(SQLSERVER_VER)
      .dbName("my_third_app")
      .collation("SQL_Latin1_General_CP1_CS_AS")
      // .containerName("ut_sqlserver")
      // .port(1433)
      .build();

    container.start();

```


#### Localstack - `localstack/localstack`

```java
    LocalstackContainer container = LocalstackContainer.newBuilder("0.14")
      .services("dynamodb,kinesis,sns,sqs")
      //.awsRegion("ap-southeast-2")
      //.port(4566)
      //.image("localstack/localstack:0.14")
      .build();

    container.start();

    // obtain what we need ...
    AmazonDynamoDB amazonDynamoDB = container.dynamoDB();
    AmazonKinesis kinesis = container.kinesis();
    AmazonSNS sns = container.sns();
    AmazonSQS sqs = container.sqs();

    // setup - create dynamoDB tables, queues etc

```

#### LocalDynamoDB - `amazon/dynamodb-local`

```java

    LocalDynamoDBContainer container = LocalDynamoDBContainer.newBuilder("1.13.2")
      //.port(8001)
      //.containerName("ut_dynamodb")
      //.image("amazon/dynamodb-local:1.13.2")
      .build();

    // start the container (if not already started)
    container.start();

    // obtain the AWS DynamoDB client
    AmazonDynamoDB amazonDynamoDB = container.dynamoDB();
    createTable(amazonDynamoDB);

```

#### MySql

```java

    MySqlContainer container = MySqlContainer.newBuilder(MYSQL_VER)
      //.containerName("ut_mysql")
      //.port(4306)
      .dbName("my_app4")
      .characterSet("utf8mb4")
      .collation("utf8mb4_unicode_ci")
      .build();

    container.start();

```

#### MariaDB

```java
    MariaDBContainer container = MariaDBContainer.newBuilder("latest")
      .dbName("my_app5")
      //.port(4306)
      .build();

    container.start();
```

#### ElasticSearch

```java
    ElasticContainer container = ElasticContainer.newBuilder("5.6.0")
      .build();

      container.start();
```

#### Redis

```java
    RedisContainer container = RedisContainer.newBuilder("latest")
      .build();

      container.start();
```

#### Oracle

```java
  OracleContainer container = OracleContainer.newBuilder("latest")
    .user("my_unique_user")
    .stopMode(StopMode.NONE)
    .build();

  container.start();
```


#### DB2

```java
    Db2Container container = Db2Container.newBuilder("11.5.4.0")
      .dbName("my_app6")
      .port(50050)
      .build();

    container.start();
```

#### Hana

```java
  // TODO ...
```


#### Clickhouse

```java
  ClickHouseContainer container = ClickHouseContainer.newBuilder("latest")
    .build();

  container.start();
```

#### Yugabyte

```java
    YugabyteContainer container = YugabyteContainer.newBuilder("2.11.2.0-b89")
      //.port(6433)
      .dbName("my_app7")
      .extensions("pgcrypto")
      .build();

    container.start();
```

## Ebean ORM use

Refer to the ebean testing documentation (https://ebean.io/docs/testing/) ...
where we use ebean-test to hook into the Ebean lifecycle and automatically
start the docker containers as needed (prior to running tests etc).

