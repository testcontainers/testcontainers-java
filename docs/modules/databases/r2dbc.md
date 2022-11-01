# R2DBC support

You can obtain a temporary database in one of two ways:

 * **Using a specially modified R2DBC URL**: after making a very simple modification to your system's R2DBC URL string, Testcontainers will provide a disposable stand-in database that can be used without requiring modification to your application code.
 * **JUnit @Rule/@ClassRule**: this mode starts a database inside a container before your tests and tears it down afterwards.

## Database containers launched via R2DBC URL scheme

As long as you have Testcontainers and the appropriate R2DBC driver on your classpath, you can simply modify regular R2DBC connection URLs to get a fresh containerized instance of the database each time your application starts up.

The started container will be terminated when the `ConnectionFactory` is closed.

!!! warning
    Both the database module (e.g. `org.testcontainers:mysql`) **and** `org.testcontainers:r2dbc` need to be on your application's classpath at runtime.

**Original URL**: `r2dbc:mysql://localhost:3306/databasename`

1. Insert `tc:` after `r2dbc:` as follows. Note that the hostname, port and database name will be ignored; you can leave these as-is or set them to any value.
1. Specify the mandatory Docker tag of the database's official image that you want using a `TC_IMAGE_TAG` query parameter.

**Note that, unlike Testcontainers' JDBC URL support, it is not possible to specify an image tag in the 'scheme' part of the URL, and it is always necessary to specify a tag using `TC_IMAGE_TAG`.**

So that the URL becomes:  
`r2dbc:tc:mysql:///databasename?TC_IMAGE_TAG=5.7.34`

!!! note
    We will use `///` (host-less URIs) from now on to emphasis the unimportance of the `host:port` pair.  
    From Testcontainers' perspective, `r2dbc:mysql://localhost:3306/databasename` and `r2dbc:mysql:///databasename` is the same URI.

!!! warning
    If you're using the R2DBC URL support, there is no need to instantiate an instance of the container - Testcontainers will do it automagically.

### R2DBC URL examples

#### Using MySQL

`r2dbc:tc:mysql:///databasename?TC_IMAGE_TAG=5.7.34`

#### Using MariaDB

`r2dbc:tc:mariadb:///databasename?TC_IMAGE_TAG=10.3.6`

#### Using PostgreSQL

`r2dbc:tc:postgresql:///databasename?TC_IMAGE_TAG=9.6.8`

#### Using MSSQL:

`r2dbc:tc:sqlserver:///?TC_IMAGE_TAG=2017-CU12`

## Obtaining `ConnectionFactoryOptions` from database container objects

If you already have an instance of the database container, you can get an instance of `ConnectionFactoryOptions` from it:
<!--codeinclude--> 
[Creating `ConnectionFactoryOptions` from an instance)](../../../modules/postgresql/src/test/java/org/testcontainers/containers/PostgreSQLR2DBCDatabaseContainerTest.java) inside_block:get_options
<!--/codeinclude-->
