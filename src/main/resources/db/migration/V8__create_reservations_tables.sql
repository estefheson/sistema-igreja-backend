CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_reservations_room FOREIGN KEY (room_id) REFERENCES rooms (id)
);

CREATE TABLE reservation_ministries (
    reservation_id BIGINT NOT NULL,
    ministry_id BIGINT NOT NULL,
    CONSTRAINT pk_reservation_ministries PRIMARY KEY (reservation_id, ministry_id),
    CONSTRAINT fk_reservation_ministries_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_reservation_ministries_ministry FOREIGN KEY (ministry_id) REFERENCES ministries (id)
);
