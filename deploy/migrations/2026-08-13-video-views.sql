CREATE TABLE IF NOT EXISTS video_views (
    id BIGSERIAL PRIMARY KEY,
    video_id BIGINT NOT NULL REFERENCES course_video(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    play_count BIGINT NOT NULL DEFAULT 1,
    first_viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_video_views_video_user UNIQUE (video_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_video_views_course_video
    ON video_views(video_id);

CREATE INDEX IF NOT EXISTS idx_video_views_user
    ON video_views(user_id);
