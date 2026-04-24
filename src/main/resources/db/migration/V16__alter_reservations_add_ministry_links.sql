ALTER TABLE reservations
ADD COLUMN using_ministry_id BIGINT,
ADD COLUMN schedule_demand_ministry_id BIGINT;

ALTER TABLE reservations
ADD CONSTRAINT fk_reservations_using_ministry
FOREIGN KEY (using_ministry_id) REFERENCES ministries (id);

ALTER TABLE reservations
ADD CONSTRAINT fk_reservations_schedule_demand_ministry
FOREIGN KEY (schedule_demand_ministry_id) REFERENCES ministries (id);

UPDATE reservations r
SET using_ministry_id = source.ministry_id,
    schedule_demand_ministry_id = source.ministry_id
FROM (
    SELECT reservation_id, MIN(ministry_id) AS ministry_id
    FROM reservation_ministries
    GROUP BY reservation_id
) source
WHERE r.id = source.reservation_id
  AND r.using_ministry_id IS NULL
  AND r.schedule_demand_ministry_id IS NULL;
