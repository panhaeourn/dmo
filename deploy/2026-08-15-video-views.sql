BEGIN;

CREATE TABLE IF NOT EXISTS video_views (
    id BIGSERIAL PRIMARY KEY,
    video_id BIGINT NOT NULL REFERENCES course_video(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    view_count BIGINT NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    total_watch_seconds BIGINT NOT NULL DEFAULT 0 CHECK (total_watch_seconds >= 0),
    progress_seconds DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (progress_seconds >= 0),
    duration_seconds DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (duration_seconds >= 0),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    session_id VARCHAR(36),
    session_watch_seconds DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (session_watch_seconds >= 0),
    session_qualified BOOLEAN NOT NULL DEFAULT FALSE,
    last_position_seconds DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (last_position_seconds >= 0),
    last_event_at TIMESTAMPTZ,
    last_qualified_at TIMESTAMPTZ,
    CONSTRAINT uq_video_views_video_user UNIQUE (video_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_video_views_video_id ON video_views(video_id);
CREATE INDEX IF NOT EXISTS idx_video_views_user_id ON video_views(user_id);

COMMIT;
