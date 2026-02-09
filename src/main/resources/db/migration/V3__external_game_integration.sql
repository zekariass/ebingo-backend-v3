-- External Game Auth Tokens Table
CREATE TABLE IF NOT EXISTS external_game_auth_tokens (
    token VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    operator_id VARCHAR(100) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    game_mode VARCHAR(100),
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_auth_token_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_token_user_id ON external_game_auth_tokens(user_id);
CREATE INDEX idx_auth_token_agent_id ON external_game_auth_tokens(agent_id);
CREATE INDEX idx_auth_token_expires_at ON external_game_auth_tokens(expires_at);
CREATE INDEX idx_auth_token_status ON external_game_auth_tokens(status);

-- External Game Sessions Table
CREATE TABLE IF NOT EXISTS external_game_sessions (
    session_token VARCHAR(255) PRIMARY KEY,
    auth_token VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    operator_id VARCHAR(100) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    game_mode VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_auth_token FOREIGN KEY (auth_token) REFERENCES external_game_auth_tokens(token) ON DELETE CASCADE,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_session_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
);

CREATE INDEX idx_session_auth_token ON external_game_sessions(auth_token);
CREATE INDEX idx_session_user_id ON external_game_sessions(user_id);
CREATE INDEX idx_session_agent_id ON external_game_sessions(agent_id);
CREATE INDEX idx_session_expires_at ON external_game_sessions(expires_at);
CREATE INDEX idx_session_status ON external_game_sessions(status);

-- External Game Transactions Table
CREATE TABLE IF NOT EXISTS external_game_txns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action VARCHAR(20) NOT NULL,
    provider_transaction_id UUID NOT NULL,
    debit_id UUID,
    game_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    operator_id VARCHAR(100) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    game_mode VARCHAR(100) NOT NULL,
    amount DECIMAL(20, 9) NOT NULL,
    result DECIMAL(20, 9),
    coefficient DECIMAL(20, 9),
    is_finished BOOLEAN NOT NULL DEFAULT false,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    wallet_entry_id BIGINT,
    response_snapshot TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ext_txn_user FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_ext_txn_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    CONSTRAINT uq_action_provider_txn UNIQUE (action, provider_transaction_id)
);

CREATE INDEX idx_ext_txn_provider_txn_id ON external_game_txns(provider_transaction_id);
CREATE INDEX idx_ext_txn_debit_id ON external_game_txns(debit_id);
CREATE INDEX idx_ext_txn_game_id ON external_game_txns(game_id);
CREATE INDEX idx_ext_txn_user_id ON external_game_txns(user_id);
CREATE INDEX idx_ext_txn_agent_id ON external_game_txns(agent_id);
CREATE INDEX idx_ext_txn_action ON external_game_txns(action);
CREATE INDEX idx_ext_txn_status ON external_game_txns(status);
CREATE INDEX idx_ext_txn_created_at ON external_game_txns(created_at);

-- Add version column to wallet for optimistic locking (if not exists)
--DO $$
--BEGIN
--    IF NOT EXISTS (
--        SELECT 1 FROM information_schema.columns
--        WHERE table_name = 'wallet' AND column_name = 'version'
--    ) THEN
--        ALTER TABLE wallet ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
--    END IF;
--END $$;
