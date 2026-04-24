CREATE TABLE user_menu_permissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    menu_key VARCHAR(100) NOT NULL,
    allowed BOOLEAN NOT NULL,
    CONSTRAINT fk_user_menu_permissions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_user_menu_permissions_user_menu UNIQUE (user_id, menu_key)
);
