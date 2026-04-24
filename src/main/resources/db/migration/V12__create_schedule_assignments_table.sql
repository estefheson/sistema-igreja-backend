CREATE TABLE schedule_assignments (
    id BIGSERIAL PRIMARY KEY,
    schedule_need_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_schedule_assignments_need_member UNIQUE (schedule_need_id, member_id),
    CONSTRAINT fk_schedule_assignments_schedule_need FOREIGN KEY (schedule_need_id) REFERENCES schedule_needs (id),
    CONSTRAINT fk_schedule_assignments_member FOREIGN KEY (member_id) REFERENCES members (id)
);
