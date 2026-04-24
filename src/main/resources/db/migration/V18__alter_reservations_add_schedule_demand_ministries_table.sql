CREATE TABLE reservation_schedule_demand_ministries (
    reservation_id BIGINT NOT NULL,
    ministry_id BIGINT NOT NULL,
    CONSTRAINT pk_reservation_schedule_demand_ministries PRIMARY KEY (reservation_id, ministry_id),
    CONSTRAINT fk_reservation_schedule_demand_ministries_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_schedule_demand_ministries_ministry FOREIGN KEY (ministry_id) REFERENCES ministries (id)
);

CREATE INDEX idx_reservation_schedule_demand_ministries_reservation_id
ON reservation_schedule_demand_ministries (reservation_id);

INSERT INTO reservation_schedule_demand_ministries (reservation_id, ministry_id)
SELECT id, schedule_demand_ministry_id
FROM reservations
WHERE schedule_demand_ministry_id IS NOT NULL
ON CONFLICT (reservation_id, ministry_id) DO NOTHING;
