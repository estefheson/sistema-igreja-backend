ALTER TABLE user_menu_permissions
DROP CONSTRAINT fk_user_menu_permissions_user;

ALTER TABLE user_menu_permissions
ADD CONSTRAINT fk_user_menu_permissions_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
