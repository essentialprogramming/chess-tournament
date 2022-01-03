--Add score column in tournament_user table

DROP PROCEDURE IF EXISTS ADD_SCORE_IN_TOURNAMENT_USERS;
CREATE PROCEDURE ADD_SCORE_IN_TOURNAMENT_USERS()
language plpgsql
as $$
BEGIN
  ALTER TABLE tournament_user
  ADD COLUMN IF NOT EXISTS score double precision;
END $$;
CALL ADD_SCORE_IN_TOURNAMENT_USERS();
DROP PROCEDURE ADD_SCORE_IN_TOURNAMENT_USERS;