SELECT 'CREATE DATABASE resource_service'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'resource_service');

CREATE SEQUENCE IF NOT EXISTS RESOURCE_SEQUENCE
INCREMENT 1
START 1
CACHE 5;

CREATE TABLE IF NOT EXISTS RESOURCE (
    id bigint primary key DEFAULT nextval('RESOURCE_SEQUENCE'),
    path varchar(200) not null,
    name varchar(100) not null,
    created_date timestamp not null,
    last_modified_date timestamp,
    UNIQUE(path, name)
);

ALTER SEQUENCE IF EXISTS RESOURCE_SEQUENCE
OWNED BY RESOURCE.id;

ALTER TABLE RESOURCE ADD CONSTRAINT DATE_CHECK
CHECK (last_modified_date > created_date);
