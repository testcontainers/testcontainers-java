CREATE KEYSPACE keySpaceTest WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};

USE keySpaceTest;

CREATE TABLE catalog_category (id bigint primary key, name text);

INSERT INTO catalog_category (id, name) VALUES (1, 'test_category');