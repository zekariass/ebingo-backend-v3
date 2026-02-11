-- Migration V7: Agent Game Settings
-- Allows agents to configure which game modes are enabled

CREATE TABLE agent_game_settings (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    game_modes TEXT NOT NULL, -- Comma-separated list of game modes (e.g., "CLASSIC,TURBO,MEGA")
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_agent_game_settings_agent FOREIGN KEY (agent_id) 
        REFERENCES agents(id) ON DELETE CASCADE,
    
    -- Unique constraint: one setting record per agent
    CONSTRAINT uq_agent_game_settings_agent UNIQUE (agent_id)
);

-- Index for faster lookups by agent_id
CREATE INDEX idx_agent_game_settings_agent_id ON agent_game_settings(agent_id);

-- Comments for documentation
COMMENT ON TABLE agent_game_settings IS 'Stores agent-specific game mode configurations';
COMMENT ON COLUMN agent_game_settings.agent_id IS 'Reference to the agent';
COMMENT ON COLUMN agent_game_settings.game_modes IS 'Comma-separated list of enabled game modes';
