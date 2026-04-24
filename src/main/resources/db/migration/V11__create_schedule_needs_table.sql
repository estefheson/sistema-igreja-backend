CREATE TABLE schedule_needs (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    ministry_id BIGINT NOT NULL,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT uk_schedule_needs_reservation_ministry UNIQUE (reservation_id, ministry_id),
    CONSTRAINT fk_schedule_needs_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_schedule_needs_ministry FOREIGN KEY (ministry_id) REFERENCES ministries (id)
);
