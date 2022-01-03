DROP TABLE IF EXISTS tournament_referee CASCADE;

CREATE TABLE IF NOT EXISTS tournament_referee (
	id serial NOT NULL PRIMARY KEY,
	tournament_id int,
	referee_id int
);

ALTER TABLE tournament_referee
ADD FOREIGN KEY (referee_id) REFERENCES users(id),
ADD FOREIGN KEY (tournament_id) REFERENCES tournament(id);