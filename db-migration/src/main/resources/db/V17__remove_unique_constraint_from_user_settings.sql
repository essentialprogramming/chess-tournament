    -- Remove unique constraint from user settings table

DROP PROCEDURE IF EXISTS REMOVE_UNIQUE_CONSTRAINT;
CREATE PROCEDURE REMOVE_UNIQUE_CONSTRAINT()
    language plpgsql
as $$
DECLARE
constraint_name TEXT := (SELECT conname
                            FROM pg_constraint
                            WHERE conrelid = 'user_settings'::regclass
                              AND contype = 'u');
BEGIN

EXECUTE 'ALTER TABLE user_settings DROP CONSTRAINT '||constraint_name||';';

END $$;
CALL REMOVE_UNIQUE_CONSTRAINT();
DROP PROCEDURE REMOVE_UNIQUE_CONSTRAINT;