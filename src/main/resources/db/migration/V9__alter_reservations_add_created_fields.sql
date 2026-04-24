ALTER TABLE reservations
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE reservations
ADD COLUMN created_by_user_id BIGINT;

ALTER TABLE reservations
ADD CONSTRAINT fk_reservations_created_by_user
FOREIGN KEY (created_by_user_id) REFERENCES users (id);
