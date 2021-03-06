--Add start_date column in match table

DROP PROCEDURE IF EXISTS ADD_START_DATE_COLUMN_TO_MATCH;
CREATE PROCEDURE ADD_START_DATE_COLUMN_TO_MATCH()
    language plpgsql
as $$
BEGIN
ALTER TABLE match
    ADD COLUMN start_date timestamp;
END $$;
CALL ADD_START_DATE_COLUMN_TO_MATCH();
DROP PROCEDURE ADD_START_DATE_COLUMN_TO_MATCH;