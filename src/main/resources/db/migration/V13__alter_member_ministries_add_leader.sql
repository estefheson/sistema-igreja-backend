ALTER TABLE member_ministries
    ADD COLUMN id BIGINT;

CREATE SEQUENCE seq_member_ministries_id START WITH 1 INCREMENT BY 1;

UPDATE member_ministries
SET id = nextval('seq_member_ministries_id')
WHERE id IS NULL;

DO $$
DECLARE
    max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0)
    INTO max_id
    FROM member_ministries;

    IF max_id = 0 THEN
        PERFORM setval('seq_member_ministries_id', 1, false);
    ELSE
        PERFORM setval('seq_member_ministries_id', max_id, true);
    END IF;
END $$;

ALTER TABLE member_ministries
    ALTER COLUMN id SET DEFAULT nextval('seq_member_ministries_id');

ALTER TABLE member_ministries
    ALTER COLUMN id SET NOT NULL;

ALTER TABLE member_ministries
    ADD COLUMN leader BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE member_ministries
    DROP CONSTRAINT pk_member_ministries;

ALTER TABLE member_ministries
    ADD CONSTRAINT pk_member_ministries PRIMARY KEY (id);

ALTER TABLE member_ministries
    ADD CONSTRAINT uk_member_ministries_member_ministry UNIQUE (member_id, ministry_id);
