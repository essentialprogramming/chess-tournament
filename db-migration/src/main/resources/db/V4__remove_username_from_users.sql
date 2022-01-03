-- Remove username column from users table

DROP PROCEDURE IF EXISTS REMOVE_USERNAME_FROM_USERS;
CREATE PROCEDURE REMOVE_USERNAME_FROM_USERS()
language plpgsql
as $$
BEGIN
  ALTER TABLE users
  DROP COLUMN IF EXISTS username;
END $$;
CALL REMOVE_USERNAME_FROM_USERS();
DROP PROCEDURE REMOVE_USERNAME_FROM_USERS;