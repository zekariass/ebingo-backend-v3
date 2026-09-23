-- Golden Eggs Bonus System Tables

-- Table: golden_eggs_bonus
-- Stores bonus information for users
CREATE TABLE golden_eggs_bonus (
    bonus_id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    sub_operator_id UUID NOT NULL,
    game_modes TEXT NOT NULL, -- JSON array of game modes
    currency VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'FREEBET',
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    bonus_quantity INT NOT NULL,
    bonus_available INT NOT NULL,
    win_sum DECIMAL(20, 9) DEFAULT 0,
    freebet_config TEXT NOT NULL, -- JSON string of FreebetConfig
    expires_at TIMESTAMP NOT NULL,
    expires_when_active_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bonus_user FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_bonus_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
);

-- Indexes for golden_eggs_bonus
CREATE INDEX idx_bonus_sub_operator ON golden_eggs_bonus(sub_operator_id);
CREATE INDEX idx_bonus_user ON golden_eggs_bonus(user_id);
CREATE INDEX idx_bonus_agent_id ON golden_eggs_bonus(agent_id);
CREATE INDEX idx_bonus_status ON golden_eggs_bonus(status);
CREATE INDEX idx_bonus_expires_at ON golden_eggs_bonus(expires_at);
CREATE INDEX idx_bonus_user_status ON golden_eggs_bonus(user_id, status);

-- Table: golden_eggs_bonus_transaction
-- Tracks bonus-related transactions (complete, expired)
CREATE TABLE golden_eggs_bonus_transaction (
    id UUID PRIMARY KEY,
    bonus_id UUID NOT NULL,
    transaction_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    agent_id BIGINT,
    action VARCHAR(50) NOT NULL, -- bonus-complete, bonus-expired-when-active
    currency VARCHAR(10) NOT NULL,
    win_sum DECIMAL(20, 9) NOT NULL,
    game_mode VARCHAR(50),
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, PENDING
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bonus_txn_bonus FOREIGN KEY (bonus_id) REFERENCES golden_eggs_bonus(bonus_id) ON DELETE CASCADE,
    CONSTRAINT fk_bonus_txn_user FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_bonus_txn_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
);

-- Indexes for golden_eggs_bonus_transaction
CREATE INDEX idx_bonus_txn_transaction ON golden_eggs_bonus_transaction(transaction_id);
CREATE INDEX idx_bonus_txn_bonus ON golden_eggs_bonus_transaction(bonus_id);
CREATE INDEX idx_bonus_txn_user ON golden_eggs_bonus_transaction(user_id);
CREATE INDEX idx_bonus_txn_agent_id ON golden_eggs_bonus_transaction(agent_id);
CREATE INDEX idx_bonus_txn_status ON golden_eggs_bonus_transaction(status);

-- Comments for documentation
COMMENT ON TABLE golden_eggs_bonus IS 'Stores Golden Eggs bonus information for users';
COMMENT ON COLUMN golden_eggs_bonus.bonus_id IS 'Unique bonus identifier (UUID)';
COMMENT ON COLUMN golden_eggs_bonus.sub_operator_id IS 'Sub-operator ID generated from aggregatorId:subId';
COMMENT ON COLUMN golden_eggs_bonus.game_modes IS 'JSON array of applicable game modes';
COMMENT ON COLUMN golden_eggs_bonus.freebet_config IS 'JSON configuration for freebet (game-specific configs)';
COMMENT ON COLUMN golden_eggs_bonus.status IS 'PENDING, CREATED, ACTIVE, PENDING_COMPLETE, COMPLETED, EXPIRED, CANCELLED, EXPIRED_WHEN_ACTIVE, FAILED';
COMMENT ON COLUMN golden_eggs_bonus.win_sum IS 'Total winnings from bonus (includes bet amount)';

COMMENT ON TABLE golden_eggs_bonus_transaction IS 'Tracks bonus completion and expiration transactions';
COMMENT ON COLUMN golden_eggs_bonus_transaction.action IS 'bonus-complete or bonus-expired-when-active';
COMMENT ON COLUMN golden_eggs_bonus_transaction.transaction_id IS 'Unique transaction ID from provider (for idempotency)';
