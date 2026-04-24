ALTER TABLE schedule_assignments
ADD COLUMN assigned_by_user_id BIGINT;

ALTER TABLE schedule_assignments
ADD CONSTRAINT fk_schedule_assignments_assigned_by_user
FOREIGN KEY (assigned_by_user_id) REFERENCES users (id);
