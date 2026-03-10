-- Activity Metrics (métricas por actividad)
CREATE TABLE IF NOT EXISTS activity_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id UUID NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    
    -- Basic metrics (raw units)
    total_distance DECIMAL(10,2) NOT NULL,        -- meters
    total_duration INTEGER NOT NULL,              -- seconds
    total_elapsed_time INTEGER,                   -- seconds
    total_calories INTEGER,                       -- kcal
    
    -- Pace & Speed (calculated)
    average_pace INTEGER,                         -- seconds per km
    max_pace INTEGER,                             -- seconds per km
    average_speed DECIMAL(6,3),                   -- m/s
    max_speed DECIMAL(6,3),                       -- m/s
    average_gap INTEGER,                          -- Grade Adjusted Pace (sec/km)
    
    -- Heart Rate
    average_heart_rate INTEGER,                   -- bpm
    max_heart_rate INTEGER,                       -- bpm
    min_heart_rate INTEGER,                       -- bpm (from records)
    hr_zones JSONB,                               -- {"Z1": 0, "Z2": 2580, "Z3": 2010, "Z4": 0, "Z5": 0}
    hr_zones_percentage JSONB,                    -- {"Z1": 0, "Z2": 54, "Z3": 42, "Z4": 0, "Z5": 4}
    
    -- Cadence
    average_cadence INTEGER,                      -- spm (steps per minute)
    max_cadence INTEGER,                          -- spm
    
    -- Running Dynamics
    average_vertical_oscillation DECIMAL(5,2),   -- mm
    average_stance_time DECIMAL(6,2),            -- ms
    average_vertical_ratio DECIMAL(4,2),         -- percent
    average_step_length INTEGER,                 -- mm
    
    -- Power (if available)
    average_power INTEGER,                       -- watts
    max_power INTEGER,                           -- watts
    normalized_power INTEGER,                    -- watts
    power_zones JSONB,                           -- similar to hr_zones
    
    -- Elevation
    total_ascent INTEGER,                        -- meters
    total_descent INTEGER,                       -- meters
    
    -- Training Load
    training_effect DECIMAL(3,1),
    anaerobic_training_effect DECIMAL(3,1),
    training_load_peak DECIMAL(10,2),
    
    -- Subjective (from FIT if available)
    workout_feel INTEGER,                        -- 0-100
    workout_rpe INTEGER,                         -- 0-100
    
    -- Metadata
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activity_metrics_activity_id ON activity_metrics(activity_id);
CREATE INDEX idx_activity_metrics_user_id ON activity_metrics(user_id);
CREATE INDEX idx_activity_metrics_calculated_at ON activity_metrics(calculated_at);

-- Lap Metrics (métricas por intervalo/lap)
CREATE TABLE IF NOT EXISTS lap_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id UUID NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    
    lap_number INTEGER NOT NULL,
    lap_name VARCHAR(100),
    intensity VARCHAR(20),                       -- 'warmup', 'active', 'cooldown', 'rest'
    
    -- Timing
    start_time TIMESTAMPTZ NOT NULL,
    distance DECIMAL(10,2) NOT NULL,             -- meters
    duration INTEGER NOT NULL,                   -- seconds
    calories INTEGER,                            -- kcal
    
    -- Pace & Speed
    average_pace INTEGER,                        -- sec/km
    max_pace INTEGER,                            -- sec/km
    average_speed DECIMAL(6,3),                  -- m/s
    max_speed DECIMAL(6,3),                      -- m/s
    average_gap INTEGER,                         -- sec/km
    
    -- Heart Rate
    average_heart_rate INTEGER,                  -- bpm
    max_heart_rate INTEGER,                      -- bpm
    min_heart_rate INTEGER,                      -- bpm
    
    -- Cadence
    average_cadence INTEGER,                     -- spm
    max_cadence INTEGER,                         -- spm
    
    -- Running Dynamics
    average_vertical_oscillation DECIMAL(5,2),  -- mm
    average_stance_time DECIMAL(6,2),           -- ms
    average_vertical_ratio DECIMAL(4,2),        -- percent
    average_step_length INTEGER,                -- mm
    
    -- Power
    average_power INTEGER,                      -- watts
    max_power INTEGER,                          -- watts
    normalized_power INTEGER,                   -- watts
    
    -- Elevation
    total_ascent INTEGER,                       -- meters
    total_descent INTEGER,                      -- meters
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lap_metrics_activity_id ON lap_metrics(activity_id);
CREATE INDEX idx_lap_metrics_user_id ON lap_metrics(user_id);
CREATE INDEX idx_lap_metrics_lap_number ON lap_metrics(activity_id, lap_number);

-- Activity Samples (raw GPS/HR/cadence data points from FIT RECORD messages)
CREATE TABLE IF NOT EXISTS activity_samples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id UUID NOT NULL,

    sample_timestamp TIMESTAMPTZ,                -- from FIT RECORD timestamp
    latitude DECIMAL(10,7),                      -- degrees
    longitude DECIMAL(10,7),                     -- degrees
    heart_rate INTEGER,                          -- bpm
    cadence INTEGER,                             -- spm (steps per minute)
    altitude DECIMAL(8,2),                       -- meters
    speed DECIMAL(7,4),                          -- m/s
    power INTEGER,                               -- watts
    distance DECIMAL(12,3),                      -- meters (accumulated)

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activity_samples_activity_id ON activity_samples(activity_id);
CREATE INDEX idx_activity_samples_timestamp ON activity_samples(activity_id, sample_timestamp);

-- Weekly Aggregated Metrics
CREATE TABLE IF NOT EXISTS weekly_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL,
    week_start_date DATE NOT NULL,               -- Monday of the week
    
    total_activities INTEGER NOT NULL DEFAULT 0,
    total_distance DECIMAL(10,2) NOT NULL DEFAULT 0,     -- meters
    total_duration INTEGER NOT NULL DEFAULT 0,           -- seconds
    total_calories INTEGER NOT NULL DEFAULT 0,           -- kcal
    total_ascent INTEGER NOT NULL DEFAULT 0,             -- meters
    
    average_heart_rate INTEGER,                  -- bpm (weighted)
    average_cadence INTEGER,                     -- spm (weighted)
    average_pace INTEGER,                        -- sec/km (weighted)
    
    training_load DECIMAL(10,2),                -- sum of training effects
    longest_run_distance DECIMAL(10,2),         -- meters
    longest_run_duration INTEGER,               -- seconds
    
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    UNIQUE(user_id, week_start_date)
);

CREATE INDEX idx_weekly_metrics_user_id ON weekly_metrics(user_id);
CREATE INDEX idx_weekly_metrics_week_start ON weekly_metrics(week_start_date);