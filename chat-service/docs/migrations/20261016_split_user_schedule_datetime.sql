BEGIN;

ALTER TABLE user_schedule
    ADD COLUMN IF NOT EXISTS start_dt VARCHAR(8),
    ADD COLUMN IF NOT EXISTS start_tim VARCHAR(6),
    ADD COLUMN IF NOT EXISTS end_dt VARCHAR(8),
    ADD COLUMN IF NOT EXISTS end_tim VARCHAR(6);

UPDATE user_schedule
SET start_dt = COALESCE(start_dt, to_char(start_at, 'YYYYMMDD')),
    start_tim = COALESCE(start_tim, to_char(start_at, 'HH24MISS')),
    end_dt = COALESCE(end_dt, to_char(end_at, 'YYYYMMDD')),
    end_tim = COALESCE(end_tim, to_char(end_at, 'HH24MISS'));

ALTER TABLE user_schedule
    DROP COLUMN start_at,
    DROP COLUMN end_at;

COMMIT;
