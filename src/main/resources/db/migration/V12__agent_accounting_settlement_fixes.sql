-- Agent accounting settlement fixes
-- 1. last_settled_at should be NULL until a settlement actually occurs
--    (previously written as NOW() on every insert, making it meaningless)
-- 2. next_settlement_time was never used — dropped

ALTER TABLE total_agent_accounting
    ALTER COLUMN last_settled_at DROP NOT NULL;

ALTER TABLE total_agent_accounting
    DROP COLUMN IF EXISTS next_settlement_time;

-- Clear the bogus NOW() values on rows that were never actually settled
UPDATE total_agent_accounting
SET last_settled_at = NULL
WHERE total_settled_amount = 0;
