CREATE TABLE bar (
  foo VARCHAR(255)
);

INSERT INTO bar (foo) VALUES ('hello world');

DROP PROCEDURE IF EXISTS count_foo;

CREATE PROCEDURE count_foo()
  BEGIN

    BEGIN
      SELECT *
      FROM bar;
      SELECT 1
      FROM dual;
    END;

    BEGIN
      select * from bar;
    END;

    -- we can do comments

    /* including block
       comments
     */

    /* what if BEGIN appears inside a comment? */

    select "or what if BEGIN appears inside a literal?";

  END;