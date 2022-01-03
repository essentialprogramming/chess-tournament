-- Insert role for super admin

DROP PROCEDURE IF EXISTS INSERT_SUPER_ADMIN_ROLE;
CREATE PROCEDURE INSERT_SUPER_ADMIN_ROLE()
    language plpgsql
as $$
DECLARE
superadmin_email VARCHAR := 'superadminavgchess@avangarde-software.com';
superadmin_id INT := (SELECT u.id FROM users u WHERE u.email = superadmin_email);
BEGIN

INSERT INTO user_platform(roles, platform, user_id) VALUES('SUPER_ADMIN', 'SUPER_ADMIN', superadmin_id);

END $$;
CALL INSERT_SUPER_ADMIN_ROLE();
DROP PROCEDURE INSERT_SUPER_ADMIN_ROLE;