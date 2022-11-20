INSERT INTO RESOURCE(id, path, name, created_date) VALUES(
    nextval('RESOURCE_SEQUENCE'), 'example/mess-code.mp3', 'mess-code.mp3', '2022-10-10T19:00');
INSERT INTO RESOURCE(id, path, name, created_date, last_modified_date) VALUES(
    nextval('RESOURCE_SEQUENCE'), 'example/clean-code.mp3', 'clean-code.mp3', '2022-10-10T19:01', '2022-10-10T19:08');
INSERT INTO RESOURCE(id, path, name, created_date) VALUES(
    nextval('RESOURCE_SEQUENCE'), 'example/dirty-code.mp3', 'dirty-code.mp3', '2022-10-10T19:02');
INSERT INTO RESOURCE(id, path, name, created_date, last_modified_date) VALUES(
    nextval('RESOURCE_SEQUENCE'), 'example/dress-code.mp3', 'dress-code.mp3', '2022-10-10T19:03', '2022-10-10T19:15');
INSERT INTO RESOURCE(id, path, name, created_date) VALUES(
    nextval('RESOURCE_SEQUENCE'), 'example/postal-code.mp3', 'postal-code.mp3', '2022-10-10T19:04');