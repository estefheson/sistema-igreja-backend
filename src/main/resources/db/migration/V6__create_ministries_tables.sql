CREATE TABLE ministries (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_ministries_name UNIQUE (name)
);

CREATE TABLE member_ministries (
    member_id BIGINT NOT NULL,
    ministry_id BIGINT NOT NULL,
    CONSTRAINT pk_member_ministries PRIMARY KEY (member_id, ministry_id),
    CONSTRAINT fk_member_ministries_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_member_ministries_ministry FOREIGN KEY (ministry_id) REFERENCES ministries (id)
);
