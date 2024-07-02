CREATE KEYSPACE keySpaceTest WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};

USE keySpaceTest;

/* The following statement contains an error (missing primary key) on purpose, do not fix it! */
CREATE TABLE catalog_category (id bigint);
