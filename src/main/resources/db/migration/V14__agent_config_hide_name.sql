-- Per-agent flag: when TRUE the client hides player display names
-- (e.g. in game/player lists) for this agent's brand.
ALTER TABLE agent_config
    ADD COLUMN IF NOT EXISTS hide_name BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN agent_config.hide_name IS 'When true, clients hide player display names for this agent';
