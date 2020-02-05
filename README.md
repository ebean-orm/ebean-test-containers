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
- HANA (via https://store.docker.com/images/sap-hana-express-edition)




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



## Ebean ORM use

Refer to the ebean testing documentation (https://ebean.io/docs/testing/) ...
where we use ebean-test to hook into the Ebean lifecycle and automatically
start the docker containers as needed (prior to running tests etc).

