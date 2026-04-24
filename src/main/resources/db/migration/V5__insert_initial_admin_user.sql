INSERT INTO users (username, email, password, active)
VALUES (
    'admin',
    'admin@igreja.local',
    '$2a$10$Hm0ETTiyNwnlm.fIARfPPuXayMCnoX3E75F11mMY2zlCeduYaca.2',
    TRUE
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.username = 'admin'
ON CONFLICT (user_id, role_id) DO NOTHING;
