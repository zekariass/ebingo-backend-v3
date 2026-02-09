-- Golden Eggs Accounting Tables

-- Table: golden_eggs_total_accounting
-- Stores cumulative accounting data across all time
CREATE TABLE golden_eggs_total_accounting (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT UNIQUE,
    total_bets_count BIGINT NOT NULL DEFAULT 0,
    total_bets_amount DECIMAL(20, 9) NOT NULL DEFAULT 0,
    total_wins_amount DECIMAL(20, 9) NOT NULL DEFAULT 0,
    total_loss_amount DECIMAL(20, 9) NOT NULL DEFAULT 0,
    total_net_profit_amount DECIMAL(20, 9) NOT NULL DEFAULT 0,
    total_rollback_count BIGINT NOT NULL DEFAULT 0,
    total_rollback_amount DECIMAL(20, 9) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_external_total_accounting_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
);

-- Index for golden_eggs_total_accounting
CREATE UNIQUE INDEX idx_external_total_accounting_agent_id ON golden_eggs_total_accounting(agent_id);

-- Table: golden_eggs_daily_accounting
-- Stores daily accounting data
CREATE TABLE golden_eggs_daily_accounting (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT,
    accounting_date DATE NOT NULL,
    daily_bets_count BIGINT NOT NULL DEFAULT 0,
    daily_bets_amount DECIMAL(20, 9) NOT NULL DEFAULT 0,
    daily_wins_amount DECIMAL(20, 9) NOT NULL DEFAULT 0,
    daily_loss_amount DECIMAL(20, 9) NOT NULL DEFAULT 0,
    daily_net_profit_amount DECIMAL(20, 9) NOT NULL DEFAULT 0,
    daily_rollback_count BIGINT NOT NULL DEFAULT 0,
    daily_rollback_amount DECIMAL(20, 9) NOT NULL DEFAULT 0,
    is_settled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_accounting_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    CONSTRAINT unique_external_daily_accounting_agent_date UNIQUE (agent_id, accounting_date)
);

-- Indexes for golden_eggs_daily_accounting
CREATE INDEX idx_external_daily_accounting_agent_id ON golden_eggs_daily_accounting(agent_id);
CREATE INDEX idx_external_daily_accounting_date ON golden_eggs_daily_accounting(accounting_date DESC);
CREATE INDEX idx_external_daily_accounting_settled ON golden_eggs_daily_accounting(is_settled);
CREATE INDEX idx_external_daily_accounting_date_settled ON golden_eggs_daily_accounting(accounting_date DESC, is_settled);

-- Comments for documentation
COMMENT ON TABLE golden_eggs_total_accounting IS 'Cumulative accounting for Golden Eggs external games';
COMMENT ON COLUMN golden_eggs_total_accounting.agent_id IS 'Agent ID for multi-tenancy (one record per agent)';
COMMENT ON COLUMN golden_eggs_total_accounting.total_bets_count IS 'Total number of bets placed';
COMMENT ON COLUMN golden_eggs_total_accounting.total_bets_amount IS 'Total amount wagered';
COMMENT ON COLUMN golden_eggs_total_accounting.total_wins_amount IS 'Total amount won by players';
COMMENT ON COLUMN golden_eggs_total_accounting.total_loss_amount IS 'Total amount paid out to winners (same as total wins amount)';
COMMENT ON COLUMN golden_eggs_total_accounting.total_net_profit_amount IS 'Net profit (bets - wins, positive = platform profit)';
COMMENT ON COLUMN golden_eggs_total_accounting.total_rollback_count IS 'Total number of rollbacks';
COMMENT ON COLUMN golden_eggs_total_accounting.total_rollback_amount IS 'Total amount rolled back';

COMMENT ON TABLE golden_eggs_daily_accounting IS 'Daily accounting for Golden Eggs external games';
COMMENT ON COLUMN golden_eggs_daily_accounting.agent_id IS 'Agent ID for multi-tenancy';
COMMENT ON COLUMN golden_eggs_daily_accounting.accounting_date IS 'Date for this accounting record';
COMMENT ON COLUMN golden_eggs_daily_accounting.daily_bets_count IS 'Number of bets for this day';
COMMENT ON COLUMN golden_eggs_daily_accounting.daily_bets_amount IS 'Amount wagered for this day';
COMMENT ON COLUMN golden_eggs_daily_accounting.daily_wins_amount IS 'Amount won by players for this day';
COMMENT ON COLUMN golden_eggs_daily_accounting.daily_loss_amount IS 'Daily amount paid out to winners (same as daily wins amount)';
COMMENT ON COLUMN golden_eggs_daily_accounting.daily_net_profit_amount IS 'Daily net profit (bets - wins, positive = platform profit)';
COMMENT ON COLUMN golden_eggs_daily_accounting.daily_rollback_count IS 'Number of rollbacks for this day';
COMMENT ON COLUMN golden_eggs_daily_accounting.daily_rollback_amount IS 'Amount rolled back for this day';
COMMENT ON COLUMN golden_eggs_daily_accounting.is_settled IS 'Whether this day has been settled/reconciled';

-- Note: Total accounting records will be created automatically per agent when first transaction occurs
