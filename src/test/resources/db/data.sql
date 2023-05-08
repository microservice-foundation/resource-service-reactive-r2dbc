DELETE from RESOURCES;

INSERT INTO RESOURCES(key, name, created_date)
    VALUES('mess-code.mp3', 'mess-code.mp3', '2022-10-10T19:00');

INSERT INTO RESOURCES(key, name, created_date, last_modified_date)
    VALUES('clean-code.mp3', 'clean-code.mp3', '2022-10-10T19:01', '2022-10-10T19:08');

INSERT INTO RESOURCES(key, name, created_date)
    VALUES('dirty-code.mp3', 'dirty-code.mp3', '2022-10-10T19:02');

INSERT INTO RESOURCES(key, name, created_date, last_modified_date)
    VALUES('dress-code.mp3', 'dress-code.mp3', '2022-10-10T19:03', '2022-10-10T19:15');

INSERT INTO RESOURCES(id, key, name, created_date)
    VALUES(123, 'postal-code.mp3', 'postal-code.mp3', '2022-10-10T19:04');