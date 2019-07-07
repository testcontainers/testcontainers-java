CREATE TABLE bar (foo VARCHAR(255));

CREATE TABLE gender (gender VARCHAR(255));
CREATE TABLE ending (ending VARCHAR(255));
CREATE TABLE end2 (end2 VARCHAR(255));
CREATE TABLE end_2 (end2 VARCHAR(255));

BEGIN
  INSERT INTO ending values ('ending');
END;

BEGIN
  INSERT INTO ending values ('ending');
END/*hello*/;

BEGIN--Hello
  INSERT INTO ending values ('ending');
END;

/*Hello*/BEGIN
  INSERT INTO ending values ('ending');
END;

CREATE TABLE foo (bar VARCHAR(255));
