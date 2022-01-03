-- Insert a super admin for the app

DROP PROCEDURE IF EXISTS INSERT_SUPER_ADMIN;
CREATE PROCEDURE INSERT_SUPER_ADMIN()
    language plpgsql
as $$
BEGIN

INSERT INTO users(email, validated, user_key, active, deleted, created_date, password, score, type)
VALUES('superadminavgchess@avangarde-software.com',
       true,
       'superadminreservedkey123',
       true,
       false,
       '2021-10-07 12:00',
       '2XFB9Ppis2UhAqQUgW/8V4826xFHMqtZ41TGiDt/bMQT5Zc4K25TRESbMVfJZDB/rsD8TwSakrq9QRPRXEZdGA==:TGSvMW+FmT+gqFtC1pFobw==:3',
       0,
       'player');

END $$;
CALL INSERT_SUPER_ADMIN();
DROP PROCEDURE INSERT_SUPER_ADMIN;