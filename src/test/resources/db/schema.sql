DROP TABLE IF EXISTS RESOURCES CASCADE;
DROP SEQUENCE IF EXISTS RESOURCES_SEQUENCE CASCADE;
CREATE SEQUENCE IF NOT EXISTS RESOURCES_SEQUENCE as bigint;

CREATE TABLE IF NOT EXISTS RESOURCES(
    "id" bigint PRIMARY KEY DEFAULT nextval('RESOURCES_SEQUENCE'),
    "key" varchar(36) NOT NULL,
    "name" varchar(100) NOT NULL UNIQUE,
    "last_modified_date" TIMESTAMP,
    "created_date" TIMESTAMP NOT NULL
);


ALTER SEQUENCE RESOURCES_SEQUENCE OWNED BY RESOURCES."id";