CREATE TABLE bar (
  foo VARCHAR(255)
);

SELECT "a /* string literal containing comment characters like -- here";
SELECT "a 'quoting' \"scenario ` involving BEGIN keyword\" here";
SELECT * from `bar`;

-- What about a line comment containing imbalanced string delimiters? "

/* or a block comment
    containing imbalanced string delimiters?
    ' "
    */

INSERT INTO bar (foo) /* ; */ VALUES ('hello world');
