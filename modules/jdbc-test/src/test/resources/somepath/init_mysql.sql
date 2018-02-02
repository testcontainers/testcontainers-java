CREATE TABLE bar (
  foo VARCHAR(255)
);

INSERT INTO bar (foo) VALUES ('hello world');

DROP PROCEDURE IF EXISTS count_foo;

CREATE PROCEDURE count_foo()
BEGIN

select * from bar\;
select 1 from dual\;

END;