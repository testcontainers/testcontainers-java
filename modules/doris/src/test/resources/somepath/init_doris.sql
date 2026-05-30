CREATE TABLE bar (
  foo VARCHAR(255)
)
DUPLICATE KEY(foo)
DISTRIBUTED BY HASH(foo) BUCKETS 1
PROPERTIES (
  "replication_num" = "1"
);

INSERT INTO bar (foo) VALUES ('hello world');
