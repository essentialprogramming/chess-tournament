-- Create user settings table

DROP TABLE IF EXISTS user_settings CASCADE;

CREATE TABLE IF NOT EXISTS user_settings (
	id serial NOT NULL PRIMARY KEY,
	user_id int,
	tournament_id int,
	active boolean, 
	created_date timestamp,
	expiration_date timestamp,
	type varchar(300),
	unique(user_id, tournament_id)
);

ALTER TABLE user_settings 
ADD FOREIGN KEY (user_id) REFERENCES users(id),
ADD FOREIGN KEY (tournament_id) REFERENCES tournament(id);