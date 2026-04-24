INSERT INTO roles (name)
VALUES ('ROLE_ADMIN'),
       ('ROLE_LEADER'),
       ('ROLE_MEMBER')
ON CONFLICT (name) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT ur.user_id,
       new_role.id
FROM user_roles ur
JOIN roles old_role ON old_role.id = ur.role_id
JOIN roles new_role ON (
    (old_role.name = 'ADMIN' AND new_role.name = 'ROLE_ADMIN')
    OR (old_role.name = 'LEADER' AND new_role.name = 'ROLE_LEADER')
    OR (old_role.name = 'MEMBER' AND new_role.name = 'ROLE_MEMBER')
)
ON CONFLICT (user_id, role_id) DO NOTHING;

DELETE FROM user_roles
WHERE role_id IN (
    SELECT id
    FROM roles
    WHERE name IN ('ADMIN', 'LEADER', 'MEMBER')
);

DELETE FROM roles
WHERE name IN ('ADMIN', 'LEADER', 'MEMBER');
