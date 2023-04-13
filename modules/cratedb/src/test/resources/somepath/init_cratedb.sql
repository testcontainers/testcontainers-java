CREATE TABLE bar (
  foo STRING
);

INSERT INTO bar (foo) VALUES ('hello world');
REFRESH TABLE bar;
