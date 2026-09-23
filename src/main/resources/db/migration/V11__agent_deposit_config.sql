-- Migration V11: Agent Deposit Config
-- Per-agent deposit bonus/lock rules applied when crediting DEPOSIT transactions.
-- One row per agent; agents without a row fall back to the global defaults
-- defined in application.yml under the `deposit.*` keys.

CREATE TABLE agent_deposit_config (
    agent_id BIGINT PRIMARY KEY,
    bonus_rate NUMERIC(19, 8) NOT NULL DEFAULT 0,        -- Fraction of deposit amount credited as bonus
    bonus_fixed NUMERIC(19, 8) NOT NULL DEFAULT 0,       -- Flat bonus added on top of the rate
    bonus_cap_enabled BOOLEAN NOT NULL DEFAULT FALSE,    -- Whether the bonus is capped
    bonus_cap_amount NUMERIC(19, 8) NOT NULL DEFAULT 0,  -- Max bonus when cap is enabled
    lock_rate NUMERIC(19, 8) NOT NULL DEFAULT 0,         -- Fraction of (deposit + bonus) locked from withdrawal
    lock_fixed NUMERIC(19, 8) NOT NULL DEFAULT 0,        -- Flat lock added on top of the rate
    lock_cap_enabled BOOLEAN NOT NULL DEFAULT FALSE,     -- Whether the lock amount is capped
    lock_cap_amount NUMERIC(19, 8) NOT NULL DEFAULT 0,   -- Max lock when cap is enabled
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint: config is deleted with its agent
    CONSTRAINT fk_agent_deposit_config_agent FOREIGN KEY (agent_id)
        REFERENCES agents(id) ON DELETE CASCADE
);

-- Comments for documentation
COMMENT ON TABLE agent_deposit_config IS 'Per-agent deposit bonus/lock rules; absence of a row means global application.yml defaults apply';
COMMENT ON COLUMN agent_deposit_config.agent_id IS 'Reference to the agent (one config row per agent)';
COMMENT ON COLUMN agent_deposit_config.bonus_rate IS 'Deposit bonus = amount * bonus_rate + bonus_fixed';
COMMENT ON COLUMN agent_deposit_config.bonus_cap_enabled IS 'When true, deposit bonus is capped at bonus_cap_amount';
COMMENT ON COLUMN agent_deposit_config.lock_rate IS 'Lock amount = (amount + bonus) * lock_rate + lock_fixed';
COMMENT ON COLUMN agent_deposit_config.lock_cap_enabled IS 'When true, lock amount is capped at lock_cap_amount';

-- Seed existing agents with the current global defaults from application.yml
-- (bonus: rate 0.1, fixed 0, capped at 500; lock: rate 0.5, fixed 0, uncapped)
INSERT INTO agent_deposit_config (agent_id, bonus_rate, bonus_fixed, bonus_cap_enabled, bonus_cap_amount,
                                  lock_rate, lock_fixed, lock_cap_enabled, lock_cap_amount)
SELECT id, 0.1, 0, TRUE, 500, 0.5, 0, FALSE, 1000
FROM agents;
