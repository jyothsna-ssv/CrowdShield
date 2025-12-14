-- CrowdShield Database Schema

-- Table: content
CREATE TABLE IF NOT EXISTS content (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(20) CHECK (type IN ('TEXT', 'IMAGE')) NOT NULL,
    text_content TEXT,
    image_url TEXT,
    status VARCHAR(20) CHECK (status IN ('PENDING', 'PROCESSING', 'SAFE', 'FLAGGED', 'ERROR')) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Table: moderation_results
CREATE TABLE IF NOT EXISTS moderation_results (
    id UUID PRIMARY KEY,
    content_id UUID REFERENCES content(id) ON DELETE CASCADE,
    toxicity_score FLOAT,
    hate_score FLOAT,
    sexual_score FLOAT,
    violence_score FLOAT,
    overall_label VARCHAR(20) CHECK (overall_label IN ('SAFE', 'FLAGGED')),
    raw_response JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Table: moderation_rules
CREATE TABLE IF NOT EXISTS moderation_rules (
    id SERIAL PRIMARY KEY,
    toxicity_threshold FLOAT NOT NULL DEFAULT 0.7,
    hate_threshold FLOAT NOT NULL DEFAULT 0.6,
    sexual_threshold FLOAT NOT NULL DEFAULT 0.6,
    violence_threshold FLOAT NOT NULL DEFAULT 0.6,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Table: admin_actions
CREATE TABLE IF NOT EXISTS admin_actions (
    id UUID PRIMARY KEY,
    content_id UUID REFERENCES content(id) ON DELETE CASCADE,
    admin_id VARCHAR(255) NOT NULL,
    previous_label VARCHAR(20),
    new_label VARCHAR(20),
    note TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Table: moderation_jobs
CREATE TABLE IF NOT EXISTS moderation_jobs (
    id UUID PRIMARY KEY,
    content_id UUID REFERENCES content(id) ON DELETE CASCADE,
    attempts INT DEFAULT 0,
    queue_name VARCHAR(50),
    last_error TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_content_status ON content(status);
CREATE INDEX IF NOT EXISTS idx_moderation_results_content_id ON moderation_results(content_id);
CREATE INDEX IF NOT EXISTS idx_admin_actions_content_id ON admin_actions(content_id);

-- Insert default moderation rules
INSERT INTO moderation_rules (toxicity_threshold, hate_threshold, sexual_threshold, violence_threshold)
VALUES (0.7, 0.6, 0.6, 0.6)
ON CONFLICT DO NOTHING;

