-- Fix agent foreign keys on Golden Eggs bonus tables
-- V4 originally created fk_bonus_agent / fk_bonus_txn_agent referencing
-- "agent(id)", but the actual table is "agents". V4 has since been
-- corrected for fresh installs; this migration repairs databases where
-- the original V4 already ran. Fully conditional - safe to run on both.

DO $$
BEGIN
    -- golden_eggs_bonus.fk_bonus_agent
    IF to_regclass('public.golden_eggs_bonus') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu
                ON tc.constraint_name = ccu.constraint_name
                AND tc.table_schema = ccu.table_schema
            WHERE tc.constraint_name = 'fk_bonus_agent'
              AND tc.table_name = 'golden_eggs_bonus'
              AND tc.constraint_type = 'FOREIGN KEY'
              AND ccu.table_name = 'agent'
        ) THEN
            ALTER TABLE golden_eggs_bonus DROP CONSTRAINT fk_bonus_agent;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_bonus_agent'
              AND table_name = 'golden_eggs_bonus'
        ) THEN
            ALTER TABLE golden_eggs_bonus
                ADD CONSTRAINT fk_bonus_agent
                FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
        END IF;
    END IF;

    -- golden_eggs_bonus_transaction.fk_bonus_txn_agent
    IF to_regclass('public.golden_eggs_bonus_transaction') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu
                ON tc.constraint_name = ccu.constraint_name
                AND tc.table_schema = ccu.table_schema
            WHERE tc.constraint_name = 'fk_bonus_txn_agent'
              AND tc.table_name = 'golden_eggs_bonus_transaction'
              AND tc.constraint_type = 'FOREIGN KEY'
              AND ccu.table_name = 'agent'
        ) THEN
            ALTER TABLE golden_eggs_bonus_transaction DROP CONSTRAINT fk_bonus_txn_agent;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_bonus_txn_agent'
              AND table_name = 'golden_eggs_bonus_transaction'
        ) THEN
            ALTER TABLE golden_eggs_bonus_transaction
                ADD CONSTRAINT fk_bonus_txn_agent
                FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
        END IF;
    END IF;
END $$;
