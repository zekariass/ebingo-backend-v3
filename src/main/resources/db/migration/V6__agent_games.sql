-- Migration for agent_games table
-- Manages the association between agents and game categories

-- Create agent_games table
CREATE TABLE agent_games (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    game_category VARCHAR(50) NOT NULL,
    game_types TEXT,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_agent_games_agent FOREIGN KEY (agent_id) 
        REFERENCES agents(id) ON DELETE CASCADE,
    
    -- Unique constraint: one entry per agent-game category combination
    CONSTRAINT uk_agent_games_agent_category UNIQUE (agent_id, game_category)
);

-- Create indexes for performance
CREATE INDEX idx_agent_games_agent_id ON agent_games(agent_id);
CREATE INDEX idx_agent_games_game_category ON agent_games(game_category);
CREATE INDEX idx_agent_games_is_enabled ON agent_games(is_enabled);

-- Add comments
COMMENT ON TABLE agent_games IS 'Association between agents and game categories';
COMMENT ON COLUMN agent_games.id IS 'Primary key';
COMMENT ON COLUMN agent_games.agent_id IS 'Reference to agent table';
COMMENT ON COLUMN agent_games.game_category IS 'Category of games (BINGO, EXTERNAL_GAMES)';
COMMENT ON COLUMN agent_games.game_types IS 'Comma-separated list of specific game types within the category';
COMMENT ON COLUMN agent_games.is_enabled IS 'Whether this game category is enabled for the agent';
COMMENT ON COLUMN agent_games.created_at IS 'Timestamp when record was created';
COMMENT ON COLUMN agent_games.updated_at IS 'Timestamp when record was last updated';
