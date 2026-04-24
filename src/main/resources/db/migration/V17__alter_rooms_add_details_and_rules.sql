ALTER TABLE rooms
ADD COLUMN capacity INTEGER,
ADD COLUMN usage_rules TEXT;

CREATE TABLE room_photos (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    image_url TEXT NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT fk_room_photos_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
);

CREATE INDEX idx_room_photos_room_id
ON room_photos (room_id);

CREATE TABLE room_reservation_rules (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL,
    start_time TIME,
    end_time TIME,
    CONSTRAINT fk_room_reservation_rules_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT uk_room_reservation_rules_room_day UNIQUE (room_id, day_of_week)
);

CREATE INDEX idx_room_reservation_rules_room_id
ON room_reservation_rules (room_id);
