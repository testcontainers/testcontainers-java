CREATE TABLE bar (
  foo VARCHAR(255)
);

INSERT INTO bar (foo) VALUES ('hello world');

CREATE FUNCTION hi_lo(
    a  NUMERIC,
    b  NUMERIC,
    c  NUMERIC,
    OUT hi NUMERIC,
    OUT lo NUMERIC)
AS $$
BEGIN
    hi := GREATEST(a, b, c);
    lo := LEAST(a, b, c);
END; $$
    LANGUAGE plpgsql;

