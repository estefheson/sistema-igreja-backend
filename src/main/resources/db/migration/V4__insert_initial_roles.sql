INSERT INTO roles (name)
VALUES ('ADMIN'),
       ('LEADER')
ON CONFLICT (name) DO NOTHING;
