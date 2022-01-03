DROP TABLE IF EXISTS user_platform CASCADE;
DROP TABLE IF EXISTS tournament_user CASCADE;
DROP TABLE IF EXISTS match_player CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS match_result CASCADE;
DROP TABLE IF EXISTS match CASCADE;
DROP TABLE IF EXISTS round CASCADE;
DROP TABLE IF EXISTS schedule CASCADE;
DROP TABLE IF EXISTS tournament CASCADE;


CREATE TABLE IF NOT EXISTS users (
    id serial NOT NULL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    firstname VARCHAR(30),
    lastname VARCHAR(30),
    email VARCHAR(50) NOT NULL,
    phone VARCHAR(30),
    default_language_id int,
    validated boolean,
    user_key VARCHAR(50),
    history_user_id int,
    active boolean,
    deleted boolean,
    created_date timestamp,
    modified_date timestamp,
    created_by VARCHAR(30),
    modified_by VARCHAR(30),
    log_key VARCHAR(30),
    password VARCHAR(200),
    score double precision,
    type VARCHAR(15),
    unique(username),
    unique(email)
    );

CREATE TABLE IF NOT EXISTS user_platform (
    id serial NOT NULL PRIMARY KEY,
    roles VARCHAR(100) NOT NULL,
    platform VARCHAR(15) NOT NULL,
    user_id int NOT NULL
    );

CREATE TABLE IF NOT EXISTS match(

    id serial PRIMARY KEY NOT NULL,
    match_key varchar(300),
    round_id int NOT NULL,
    tournament_id int NOT NULL,
    match_result_id int,
    state varchar(10)
    );

CREATE TABLE IF NOT EXISTS match_player
(
    match_player_key varchar(300),
    first_player_id integer NOT NULL,
    second_player_id integer NOT NULL,
    tournament_id integer NOT NULL,
    match_id integer NOT NULL
);

CREATE TABLE schedule(
    id serial NOT NULL PRIMARY KEY,
    start_date timestamp,
    end_date timestamp,
    location varchar(50)
);

CREATE TABLE tournament(
    id serial NOT NULL PRIMARY KEY,
    schedule_id int,
    name varchar(30),
    state varchar(10)
);

CREATE TABLE round (
    id serial PRIMARY KEY  NOT NULL,
    round_key varchar(300),
    tournament_id int,
    number int
);

CREATE TABLE match_result (
     id serial PRIMARY KEY  NOT NULL,
     match_result_key varchar(300),
     first_player_id int,
     second_player_id int,
     first_player_result varchar(10),
     second_player_result varchar(10),
     result varchar(10)
);

CREATE TABLE tournament_user (
     tournament_id int,
     user_id int
);

ALTER TABLE users
    ALTER COLUMN score TYPE double precision,
    ALTER COLUMN username TYPE varchar(50),
    ALTER COLUMN email TYPE varchar(50);


ALTER TABLE user_platform
    ADD FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE match
    ADD FOREIGN KEY (match_result_id)
    REFERENCES match_result (id);


ALTER TABLE match
    ADD FOREIGN KEY (round_id)
    REFERENCES round (id);


ALTER TABLE match
    ADD FOREIGN KEY (tournament_id)
    REFERENCES tournament (id);


ALTER TABLE match_player
    ADD UNIQUE (first_player_id, second_player_id),
    ADD FOREIGN KEY (first_player_id) REFERENCES users(id),
    ADD FOREIGN KEY (second_player_id) REFERENCES users(id),
    ADD FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    ADD FOREIGN KEY (match_id) REFERENCES match (id);


ALTER TABLE tournament
    ADD FOREIGN KEY (schedule_id) REFERENCES schedule (id);


ALTER TABLE round
    ADD FOREIGN KEY (tournament_id) REFERENCES tournament(id);


ALTER TABLE match_result
    ADD FOREIGN KEY (first_player_id) REFERENCES users(id),
    ADD FOREIGN KEY (second_player_id) REFERENCES users(id);


ALTER TABLE tournament_user
    ADD CONSTRAINT fk_tournament FOREIGN KEY (tournament_id)
    REFERENCES tournament (id),
    ADD CONSTRAINT fk_user FOREIGN KEY (user_id)
    REFERENCES users (id),
    ADD unique(tournament_id, user_id);
