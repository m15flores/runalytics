-- Enable PGVector extension
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Activities table (normalized data)
CREATE TABLE IF NOT EXISTS activities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(50) NOT NULL,
    device VARCHAR(100),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_seconds INTEGER,
    distance_meters NUMERIC(10, 2),
    raw_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_activities_user_started ON activities(user_id, started_at DESC);
CREATE INDEX idx_activities_created ON activities(created_at DESC);

-- Activity metrics table
CREATE TABLE IF NOT EXISTS activity_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    hr_zone_distribution JSONB,
    gap_avg_pace_s_per_km NUMERIC(6, 2),
    trimp_score NUMERIC(10, 2),
    running_power_avg_w NUMERIC(6, 2),
    effort_score NUMERIC(5, 2),
    calculated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_metrics_activity ON activity_metrics(activity_id);
CREATE INDEX idx_metrics_calculated ON activity_metrics(calculated_at DESC);

-- Weekly aggregates table
CREATE TABLE IF NOT EXISTS weekly_aggregates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(50) NOT NULL,
    week_start DATE NOT NULL,
    week_end DATE NOT NULL,
    total_distance_km NUMERIC(10, 2),
    total_duration_seconds INTEGER,
    activity_count INTEGER,
    avg_hr INTEGER,
    total_training_load NUMERIC(10, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, week_start)
);

CREATE INDEX idx_weekly_user_week ON weekly_aggregates(user_id, week_start DESC);

-- AI insights table
CREATE TABLE IF NOT EXISTS ai_insights (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(50) NOT NULL,
    insight_type VARCHAR(50),
    tone VARCHAR(20),
    summary TEXT,
    metrics JSONB,
    recommendations JSONB,
    explanations JSONB,
    embedding vector(1536),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_insights_user_created ON ai_insights(user_id, created_at DESC);
CREATE INDEX idx_insights_type ON ai_insights(insight_type);
CREATE INDEX idx_insights_embedding ON ai_insights USING ivfflat (embedding vector_cosine_ops);