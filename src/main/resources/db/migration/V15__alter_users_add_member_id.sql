ALTER TABLE users
    ADD COLUMN member_id BIGINT;

INSERT INTO members (
    full_name,
    cpf,
    birth_date,
    email,
    description,
    active
)
SELECT
    u.username,
    'USR-' || u.id,
    DATE '1900-01-01',
    u.email,
    'Membro criado automaticamente para vincular usuario existente',
    TRUE
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM members m
    WHERE m.cpf = 'USR-' || u.id
);

UPDATE users u
SET member_id = m.id
FROM members m
WHERE u.member_id IS NULL
  AND m.cpf = 'USR-' || u.id;

ALTER TABLE users
    ALTER COLUMN member_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_member_id UNIQUE (member_id);

ALTER TABLE users
    ADD CONSTRAINT fk_users_member
        FOREIGN KEY (member_id) REFERENCES members (id);
