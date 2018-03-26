# History

Note that this project has been moved and renamed from avaje/docker-commands to ebean-orm/ebean-test-docker.


# ebean-test-docker
Ability to control docker containers. e.g. Postgres running as docker container for testing.

The issues this solves for us is to:
- create databases and database users as needed
- wait for databases to be in ready state 


The needs of this project are primarily driven by the needs/desires of using docker
containers to make testing nice for Ebean ORM - https://ebean-orm.github.io/


## Supported Containers

- Postgres 
- MySql 
- SqlServer (via https://hub.docker.com/r/microsoft/mssql-server-linux/)
- Oracle (via https://hub.docker.com/r/sath89/oracle-12c/)
- ElasticSearch 




## Programmatic use
We can programmatically create the containers.
```java

String version = "9.6";
PostgresConfig config = new PostgresConfig(version);
// set some configuration options
config.setContainerName("junk_postgres");
config.setPort("9823");
config.setDbUser("rob");

PostgresContainer container = new PostgresContainer(config);

// start creating the DB and User if required
container.startWithCreate();

// start dropping and re-creating the DB and User if required
container.startWithDropCreate();

// stop the container
container.stopOnly();
```


## AutoStart use
Alternatively we can get them to run via `AutoRun`. This is a good approach
when we want to start multiple containers (like Postgres + ElasticSearch).

1. Add a docker-run.properties 
2. Execute AutoStart.run()

Example docker-run.properties

```properties

elastic.version=5.6.0

postgres.version=9.6
postgres.dbName=junk_db
postgres.dbUser=rob
postgres.dbExtensions=hstore,pgcrypto
postgres.port=6432

```

## Ebean ORM use

We use `ebean-docker-run` (https://github.com/ebean-orm/ebean-docker-run) ... to hook into the
Ebean lifecycle and automatically start the docker containers as needed (prior to running tests etc).

