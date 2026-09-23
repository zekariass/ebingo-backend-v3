-- Migration V10: Agent Config
-- Per-agent bot configuration (brand name, admin IDs, support handles, bank details)
-- Consumed by the Telegram bot server via GET /api/v1/agents/{agentId}/bot-config

CREATE TABLE agent_config (
    agent_id BIGINT PRIMARY KEY,
    brand_name TEXT,            -- Display name used in bot messages (e.g. "Redfox Bingo")
    admin_ids TEXT,             -- Comma-separated Telegram user IDs
    logo_name TEXT,             -- Static logo filename served by the bot app (e.g. "logo_redfox.png")
    support_contact TEXT,       -- Free-form support contact (email, phone, etc.)
    support_username TEXT,      -- Telegram username without @
    support_channel TEXT,       -- Telegram channel/group handle without @
    bank_details JSONB,         -- Free-form map keyed by payment method (e.g. telebirr, cbeonline)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint: config is deleted with its agent
    CONSTRAINT fk_agent_config_agent FOREIGN KEY (agent_id)
        REFERENCES agents(id) ON DELETE CASCADE
);

-- Comments for documentation
COMMENT ON TABLE agent_config IS 'Per-agent bot configuration consumed by the Telegram bot server';
COMMENT ON COLUMN agent_config.agent_id IS 'Reference to the agent (one config row per agent)';
COMMENT ON COLUMN agent_config.brand_name IS 'Display name used in bot messages';
COMMENT ON COLUMN agent_config.admin_ids IS 'Comma-separated Telegram user IDs of bot admins';
COMMENT ON COLUMN agent_config.logo_name IS 'Static logo filename served by the bot app';
COMMENT ON COLUMN agent_config.support_username IS 'Telegram support username without @';
COMMENT ON COLUMN agent_config.support_channel IS 'Telegram channel/group handle without @';
COMMENT ON COLUMN agent_config.bank_details IS 'Free-form JSON map keyed by payment method';

-- Seed data (inserted only when the referenced agent exists)

-- Agent 1: Redfox Bingo
INSERT INTO agent_config (agent_id, brand_name, admin_ids, logo_name, support_contact, support_username, support_channel, bank_details)
SELECT 1, 'Redfox Bingo', '1961597377,702124837', 'logo_redfox.png', 'support@redfoxbingo.com',
       'RedfoxSupportBot', 'redfoxbingo',
       '{"telebirr":{"recieverName":"Mulat Tarekegn Mersha","phoneNumber":"251918041046"},"cbeonline":{"accountName":"Mr Mulat Tarekegn Mersha","accountNumber":"1000736196372"}}'::jsonb
WHERE EXISTS (SELECT 1 FROM agents WHERE id = 1);

-- Agent 2: Awash Bingo
INSERT INTO agent_config (agent_id, brand_name, admin_ids, logo_name, support_contact, support_username, support_channel, bank_details)
SELECT 2, 'Awash Bingo', '1961597377,312661397', 'logo_abex.png', '',
       'AbexSupportBot', 'abexbingo',
       '{"telebirr":{"recieverName":"bezawite tadele zenebe","phoneNumber":"251902493104"},"cbeonline":{"accountName":"Bezawit Tadele Zenebe","accountNumber":"1000210696354"}}'::jsonb
WHERE EXISTS (SELECT 1 FROM agents WHERE id = 2);

-- Agent 4: Agent 3 Bingo
INSERT INTO agent_config (agent_id, brand_name, admin_ids, logo_name, support_contact, support_username, support_channel, bank_details)
SELECT 4, 'Agent 3 Bingo', '1961597377,2726262727', 'bot_hero.png', '',
       'AbexSupportBot', 'abexbingo',
       '{"telebirr":{"recieverName":"bezawite tadele zenebe","phoneNumber":"251902493104"},"cbeonline":{"accountName":"Bezawit Tadele Zenebe","accountNumber":"1000210696354"}}'::jsonb
WHERE EXISTS (SELECT 1 FROM agents WHERE id = 4);
