-- External game txn payment sources + optimistic locking on accounting tables

-- payment_sources stores the bonus-bucket breakdown used for a debit
-- (e.g. "WELCOME_BONUS/20.00*DEPOSIT_BONUS/5.00") so a rollback can
-- restore funds to the exact buckets they were taken from.
ALTER TABLE external_game_txns
    ADD COLUMN IF NOT EXISTS payment_sources TEXT;

COMMENT ON COLUMN external_game_txns.payment_sources IS 'Bucket breakdown used for the debit, replayed on rollback';

-- Optimistic locking columns for accounting entities (@Version)
ALTER TABLE golden_eggs_daily_accounting
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE golden_eggs_total_accounting
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
