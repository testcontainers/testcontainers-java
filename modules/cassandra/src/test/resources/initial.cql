CREATE KEYSPACE keySpaceTest WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};

CREATE TABLE keySpaceTest.catalog_category (id bigint primary key, name text);

INSERT INTO keySpaceTest.catalog_category (id, name) VALUES (1, 'test_category');
