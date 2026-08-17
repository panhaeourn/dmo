BEGIN;

CREATE TABLE IF NOT EXISTS r2_cleanup_jobs (
    id BIGSERIAL PRIMARY KEY,
    object_key VARCHAR(1024) NOT NULL,
    reason VARCHAR(120) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error VARCHAR(1000),
    CONSTRAINT uq_r2_cleanup_object_key UNIQUE (object_key)
);

CREATE INDEX IF NOT EXISTS idx_r2_cleanup_due ON r2_cleanup_jobs(next_attempt_at, id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'cito_user') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE r2_cleanup_jobs TO cito_user;
        GRANT USAGE, SELECT ON SEQUENCE r2_cleanup_jobs_id_seq TO cito_user;
    END IF;
END $$;

COMMIT;
