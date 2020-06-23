CREATE TABLE bar (
  foo VARCHAR(255)
);

DROP PROCEDURE IF EXISTS -- ;
    count_foo;

SELECT "a /* string literal containing comment characters like -- here";
SELECT "a 'quoting' \"scenario ` involving BEGIN keyword\" here";
SELECT * from `bar`;

-- What about a line comment containing imbalanced string delimiters? "

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

  END /*; */;

/* or a block comment
    containing imbalanced string delimiters?
    ' "
    */

INSERT INTO bar (foo) /* ; */ VALUES ('hello world');
