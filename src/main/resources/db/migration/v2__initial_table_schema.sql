CREATE TABLE agents (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    contact_name VARCHAR(100),
    is_master BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT FALSE,
    commission_rate DECIMAL(5, 2) DEFAULT 0.0,
    bot_token TEXT, -- Telegram bot token
    bot_username VARCHAR(100), -- Telegram bot username
    contact_address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agents_is_active ON agents(is_active);
CREATE UNIQUE INDEX idx_agents_is_owner ON agents(is_master) WHERE is_master = true;


CREATE TABLE game_brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    is_active BOOLEAN DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE agent_connected_game_brands (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_subscribed_games_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_subscribed_games_game FOREIGN KEY (game_id) REFERENCES game_brands(id) ON DELETE CASCADE,
    CONSTRAINT uq_agent_game UNIQUE (agent_id, game_id)
);

--=================USER PROFILE TABLE=============================
CREATE TABLE user_profile (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT NOT NULL,  -- Telegram ID is unique and immutable
    phone_number VARCHAR(20) NOT NULL,
    referrer_id BIGINT,  -- Self-referential foreign key
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50),
    nickname VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'BANNED'
    role VARCHAR(30) NOT NULL DEFAULT 'PLAYER',  -- 'PLAYER', 'MODERATOR', 'ADMIN', "AGENT"
    is_deleted BOOLEAN DEFAULT FALSE,
    is_bot BOOLEAN DEFAULT FALSE,
    bot_room_id BIGINT,
    password TEXT, -- for bank withdrawal authentication
    agent_id BIGINT NOT NULL, -- NEW
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_profile_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE RESTRICT, -- NEW
    CONSTRAINT uq_together_telegram_id_agent_id UNIQUE (telegram_id, agent_id), -- NEW
    CONSTRAINT uq_together_phone_number_agent_id UNIQUE (phone_number, agent_id) -- NEW
);

-- Indexes for user_profile
CREATE INDEX idx_user_profile_status ON user_profile(status);
CREATE INDEX idx_user_profile_role ON user_profile(role);
CREATE INDEX idx_user_profile_is_bot ON user_profile(is_bot);
CREATE INDEX idx_user_profile_agent_id ON user_profile(agent_id); -- NEW

--====================================BINGO TABLES===================
-- room Table
CREATE TABLE room (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    min_players INT NOT NULL,
    entry_fee NUMERIC(12, 2) DEFAULT 0,
    pattern VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    card_pool_json TEXT,
    all_card_ids_json TEXT,
    bot_allowed BOOLEAN DEFAULT TRUE,
    min_bots INT DEFAULT 2,
    max_bots INT DEFAULT 2,
    commission_rate NUMERIC(5, 2) DEFAULT 0.2,
    agent_id BIGINT NOT NULL, -- NEW
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_created_by FOREIGN KEY (created_by) REFERENCES user_profile(id) ON DELETE SET NULL,
    CONSTRAINT fk_room_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE -- NEW
);

-- Indexes for room
CREATE INDEX idx_room_status ON room(status);
CREATE INDEX idx_room_pattern ON room(pattern);
CREATE INDEX idx_room_created_by ON room(created_by);
CREATE INDEX idx_room_agent_id ON room(agent_id); -- NEW


CREATE TABLE game (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT,
    joined_players_ids TEXT,
    drawn_numbers TEXT,
    players_count INT NOT NULL DEFAULT 0,
    entries_count INT NOT NULL DEFAULT 0,
    prize_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    commission_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    capacity INT NOT NULL,
    entry_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    started BOOLEAN NOT NULL DEFAULT FALSE,
    ended BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'READY', -- 'READY', 'PLAYING', 'COMPLETED', 'CANCELLED'
    agent_id BIGINT NOT NULL, -- NEW
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT game_state_room FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE SET NULL,
    CONSTRAINT fk_game_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE -- NEW
);

-- Indexes for game
CREATE INDEX idx_game_room_id ON game(room_id);
CREATE INDEX idx_game_status ON game(status);
CREATE INDEX idx_game_agent_id ON game(agent_id); -- NEW


CREATE TABLE game_transaction (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT,
    player_id BIGINT,
    txn_amount NUMERIC(19, 2) NOT NULL, -- total amount involved in the transaction
    single_game_fee NUMERIC(19, 2) DEFAULT 0.0, -- for GAME_FEE type transactions, single game fee
    commission_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.0,
    txn_type VARCHAR(50) NOT NULL,  -- e.g., GAME_FEE, PRIZE_PAYOUT, REFUND, DISPUTE
    txn_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- e.g., SUCCESS, FAIL, AWAITING_APPROVAL
    agent_id BIGINT NOT NULL, -- NEW
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_game_transaction_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE SET NULL,
    CONSTRAINT fk_game_transaction_user FOREIGN KEY (player_id) REFERENCES user_profile(id) ON DELETE SET NULL,
    CONSTRAINT fk_game_transaction_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE -- NEW
);

-- Useful indexes
CREATE INDEX idx_game_transaction_game_id ON game_transaction(game_id);
CREATE INDEX idx_game_transaction_player ON game_transaction(player_id);
CREATE INDEX idx_game_transaction_status ON game_transaction(txn_status);
CREATE INDEX idx_game_transaction_type ON game_transaction(txn_type);
CREATE INDEX idx_game_transaction_agent_id ON game_transaction(agent_id); -- NEW


-- bingo_claims Table
CREATE TABLE bingo_claims (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    card TEXT,
    marked_numbers TEXT,
    pattern VARCHAR(50),
    is_winner BOOLEAN DEFAULT FALSE,
    error_message TEXT,
    agent_id BIGINT NOT NULL,
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bingo_claims_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE,
    CONSTRAINT fk_bingo_claims_user_profile FOREIGN KEY (player_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_bingo_claims_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE -- NEW
);

-- Indexes for bingo_claims
CREATE INDEX idx_bingo_claims_game_id ON bingo_claims(game_id);
CREATE INDEX idx_bingo_claims_player_id ON bingo_claims(player_id);
CREATE INDEX idx_bingo_claims_is_winner ON bingo_claims(is_winner);
CREATE INDEX idx_bingo_claims_agent_id ON bingo_claims(agent_id); -- NEW

-- payment_method Table
CREATE TABLE payment_method (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    is_online BOOLEAN DEFAULT TRUE,
    is_mobile_money BOOLEAN DEFAULT FALSE,
    instruction_url TEXT,
    logo_url TEXT,
    withdrawal_require_password BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Indexes for faster queries
CREATE INDEX idx_payment_method_name ON payment_method(name);
CREATE INDEX idx_payment_method_is_default ON payment_method(is_default);
CREATE INDEX idx_payment_method_is_online ON payment_method(is_online);
CREATE UNIQUE INDEX idx_default_payment_method ON payment_method (is_default) WHERE is_default = true;


-- ===============================
-- PAYMENT ORDER TABLE DEFINITION
-- ===============================
CREATE TABLE IF NOT EXISTS payment_order (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    txn_ref VARCHAR(50) UNIQUE NOT NULL,
    phone_number VARCHAR(30),
    provider_order_ref VARCHAR(100),
    amount NUMERIC(18, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason TEXT,
    payment_method_id BIGINT NOT NULL,
    instructions_url TEXT,
    txn_type VARCHAR(50) NOT NULL,
    nonce VARCHAR(100) NOT NULL,
    meta_data TEXT,
    approved_by BIGINT,
    agent_id BIGINT NOT NULL, -- NEW
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_method(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE, -- NEW

    CONSTRAINT uq_payment_order_txn_ref UNIQUE (txn_ref),
    CONSTRAINT uq_payment_order_provider_order_ref UNIQUE (provider_order_ref)
);

-- ===============================
-- INDEXES FOR PERFORMANCE
-- ===============================
CREATE INDEX IF NOT EXISTS idx_payment_order_user_id ON payment_order (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_order_txn_ref ON payment_order (txn_ref);
CREATE INDEX IF NOT EXISTS idx_payment_order_provider_ref ON payment_order (provider_order_ref);
CREATE INDEX IF NOT EXISTS idx_payment_order_status ON payment_order (status);
CREATE INDEX IF NOT EXISTS idx_payment_order_payment_method ON payment_order (payment_method_id);
CREATE INDEX IF NOT EXISTS idx_payment_order_txn_type ON payment_order (txn_type);
CREATE INDEX IF NOT EXISTS idx_payment_order_created_at ON payment_order (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_order_updated_at ON payment_order (updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_order_approved_by ON payment_order (approved_by);
CREATE INDEX IF NOT EXISTS idx_payment_order_agent_id ON payment_order (agent_id); -- NEW

-- ===============================
-- TRANSACTION TABLE DEFINITION
-- ===============================
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    txn_ref VARCHAR(50) UNIQUE NOT NULL,
    payment_method_id BIGINT NOT NULL,
    txn_type VARCHAR(50) NOT NULL,
    txn_amount NUMERIC(18, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    meta_data TEXT,
    agent_id BIGINT NOT NULL, -- NEW
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_transactions_player FOREIGN KEY (player_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_order FOREIGN KEY (order_id) REFERENCES payment_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_method(id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE -- NEW
);

-- ===============================
-- INDEXES FOR PERFORMANCE
-- ===============================
CREATE INDEX IF NOT EXISTS idx_transaction_player_id ON transactions (player_id);
CREATE INDEX IF NOT EXISTS idx_transaction_order_id ON transactions (order_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_transaction_txn_ref ON transactions (txn_ref);
CREATE INDEX IF NOT EXISTS idx_transaction_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_transaction_txn_type ON transactions (txn_type);
CREATE INDEX IF NOT EXISTS idx_transaction_payment_method ON transactions (payment_method_id);
CREATE INDEX IF NOT EXISTS idx_transaction_created_at ON transactions (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_updated_at ON transactions (updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_status_type_created_at ON transactions (status, txn_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_agent_id ON transactions (agent_id); -- NEW


--TODO: Make sure transfer is made between two users in same agency - in backend
CREATE TABLE deposit_transfer (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- SUCCESS, FAIL, AWAITING_APPROVAL
    agent_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deposit_transfer_sender FOREIGN KEY (sender_id) REFERENCES user_profile(id) ON DELETE RESTRICT,
    CONSTRAINT fk_deposit_transfer_receiver FOREIGN KEY (receiver_id) REFERENCES user_profile(id) ON DELETE RESTRICT,
    CONSTRAINT fk_deposit_transfer_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
);

CREATE INDEX idx_sender_id ON deposit_transfer(sender_id);
CREATE INDEX idx_receiver_id ON deposit_transfer(receiver_id);
CREATE INDEX idx_deposit_transfer_status ON deposit_transfer(status); -- NEW
CREATE INDEX idx_deposit_transfer_agent_id ON deposit_transfer(agent_id); -- NEW


CREATE TABLE wallet (
    id BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    welcome_bonus NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    available_welcome_bonus NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    referral_bonus NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    available_referral_bonus NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    total_prize_amount NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    pending_withdrawal NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    total_available_balance NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    available_to_withdraw NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    locked_amount NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    deposit_bonus NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    promotional_bonus NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    last_payment_from TEXT, -- WELCOME_BONUS/20*REFERRAL_BONUS/40*PROMOTIONAL_BONUS/60.40*etc
    agent_id BIGINT NOT NULL, -- NEW
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE, -- NEW
    CONSTRAINT uq_wallet_per_user_profile_and_agent UNIQUE (user_profile_id, agent_id)
);

CREATE INDEX idx_wallet_user_profile_id ON wallet(user_profile_id);
CREATE INDEX idx_wallet_agent_id ON wallet(agent_id); -- NEW

-- system_config Table
CREATE TABLE system_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    value TEXT NOT NULL,
    agent_id BIGINT NOT NULL, -- NEW
    possible_values TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_system_config_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE, -- NEW
    CONSTRAINT uq_system_config_name_agent UNIQUE (name, agent_id)
);

-- Indexes for system_config
CREATE INDEX idx_system_config_name ON system_config(name);
CREATE INDEX idx_system_config_agent_id ON system_config(agent_id); -- NEW


--TODO: Replace these two tables with daily_accounting and total_accounting tables
CREATE TABLE total_commission (
    id BIGSERIAL PRIMARY KEY,
    total_commission NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    total_prize NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    last_withdrawal_amount NUMERIC(18,2) DEFAULT 0.00 NOT NULL, -- last withdrawal by admin not by player
    total_withdrawal NUMERIC(18,2) DEFAULT 0.00 NOT NULL, -- withdrawal by admin not by player
    last_withdrawal_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE daily_commission (
    id BIGSERIAL PRIMARY KEY,
    commission_date DATE NOT NULL UNIQUE,
    commission_collected NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    game_count INT DEFAULT 0 NOT NULL,
    total_prize_amount NUMERIC(18,2) DEFAULT 0.00 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--=========================== NEW ============================

CREATE TABLE daily_agent_accounting (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    accounting_date DATE NOT NULL,
    daily_deposit_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_withdrawal_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_bet_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL, -- Excluding bot bets
    daily_prize_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL, -- Excluding bot wins
    daily_commission_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL, -- Excluding bot commissions and commissions from bonuses
    daily_bot_win_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL, -- Amount won by bots from real players
    daily_bot_loss_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL, -- Amount lost by bots to real players
    daily_promotional_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_welcome_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_referral_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL, -- NEW
    daily_deposit_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_net_income NUMERIC(19,2)
    GENERATED ALWAYS AS (
          daily_commission_amount -- Commission from all real players / real money (Excluding bonuses)
        + daily_bot_win_amount -- Real players game fees - commissions paid from fees (Excluding bonuses)
        - daily_bot_loss_amount  -- All bots game fee - commissions paid from fees
    ) STORED,
    settled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_agent_accounting_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    CONSTRAINT uq_daily_agent_accounting_date_agent UNIQUE (accounting_date, agent_id)
);

CREATE INDEX idx_daily_agent_accounting_date ON daily_agent_accounting(accounting_date);
CREATE INDEX idx_daily_agent_accounting_agent_id ON daily_agent_accounting(agent_id);



CREATE TABLE total_agent_accounting (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    total_deposit_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_withdrawal_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_bet_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_prize_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_commission_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_bot_win_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,   -- Amount won by bots from real players
    total_bot_loss_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,  -- Amount lost by bots to real players
    total_promotional_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_welcome_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_referral_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL, -- NEW
    total_deposit_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_net_income NUMERIC(19,2) GENERATED ALWAYS AS (
          total_commission_amount
        + total_bot_win_amount
        - total_bot_loss_amount
    ) STORED,
    last_settled_at TIMESTAMP NOT NULL,
    last_settled_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_settled_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    next_settlement_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_total_agent_accounting_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    CONSTRAINT uq_total_agent_accounting_agent UNIQUE (agent_id)
);

CREATE INDEX idx_total_accounting_agent_id ON total_agent_accounting(agent_id);


--============================================================

CREATE TABLE referral_history (
    id BIGSERIAL PRIMARY KEY,
    referrer_id BIGINT NOT NULL,
    referee_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_referrer_not_same_as_referee CHECK (referrer_id <> referee_id)
);

CREATE INDEX idx_referrer_id ON referral_history(referrer_id);
CREATE INDEX idx_referee_id ON referral_history(referee_id);


--=========================== LEADERBOARD ============================
CREATE TABLE daily_leaderboard (
    id BIGSERIAL PRIMARY KEY,
    leaderboard_date DATE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES user_profile(id),
    agent_id BIGINT NOT NULL,   -- NEW
    daily_games_played INT DEFAULT 0 NOT NULL,
    daily_wins INT DEFAULT 0 NOT NULL,
    daily_prize NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_bets NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_deposit NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_withdrawal NUMERIC(19, 2) DEFAULT 0.00 NOT NULL, -- NEW
    is_bot BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_daily_leaderboard_user_date UNIQUE (user_id, agent_id, leaderboard_date),
    CONSTRAINT fk_daily_leaderboard_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE -- NEW
);

CREATE INDEX idx_daily_leaderboard_date ON daily_leaderboard(leaderboard_date);
CREATE INDEX idx_daily_leaderboard_user_id ON daily_leaderboard(user_id);
CREATE INDEX idx_daily_leaderboard_agent_id ON daily_leaderboard(agent_id);
CREATE INDEX idx_daily_leaderboard_agent_id_user_id ON daily_leaderboard(agent_id, user_id);
CREATE INDEX idx_daily_leaderboard_agent_id_leaderboard_date ON daily_leaderboard(agent_id, leaderboard_date);
CREATE INDEX idx_daily_leaderboard_user_id_leaderboard_date ON daily_leaderboard(user_id, leaderboard_date);
CREATE INDEX idx_daily_leaderboard_agent_id_user_id_leaderboard_date ON daily_leaderboard(agent_id, user_id, leaderboard_date);

CREATE TABLE total_leaderboard (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_profile(id),
    agent_id BIGINT NOT NULL,   -- NEW
    total_games_played INT DEFAULT 0 NOT NULL,
    total_wins INT DEFAULT 0 NOT NULL,
    total_prize NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_bets NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_deposit NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_withdrawal NUMERIC(19, 2) DEFAULT 0.00 NOT NULL, -- NEW
    is_bot BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_total_leaderboard_user_per_agent UNIQUE (user_id, agent_id), -- NEW
    CONSTRAINT fk_total_leaderboard_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE -- NEW
);

CREATE INDEX idx_total_leaderboard_user_id ON total_leaderboard(user_id);
CREATE INDEX idx_total_leaderboard_agent_id ON total_leaderboard(agent_id);
CREATE INDEX idx_total_leaderboard_agent_id_user_id ON total_leaderboard(agent_id, user_id);




--========================= Guess football design ============================
--- =======================
 -- ENUM TYPES
 -- =======================
 CREATE TYPE match_status AS ENUM ('SCHEDULED', 'LIVE', 'FINISHED', 'POSTPONED', 'CANCELLED');
 CREATE TYPE bet_status AS ENUM ('PENDING', 'WON', 'LOST', 'CANCELLED');
 CREATE TYPE bet_type AS ENUM ('SINGLE', 'MULTIPLE');
 CREATE TYPE payout_type AS ENUM ('ODD_BASED', 'SHARED');
 CREATE TYPE team_type AS ENUM ('CLUB', 'NATIONAL');

 -- =======================
 -- COUNTRIES -- general for all agencies
 -- =======================
 CREATE TABLE countries (
     id BIGSERIAL PRIMARY KEY,
     name VARCHAR(100) NOT NULL UNIQUE,
     iso_code CHAR(2) UNIQUE,
     fifa_code CHAR(3) UNIQUE,
     country_flag_url TEXT,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 );

 -- =======================
 -- COMPETITIONS -- LEAGUES, CUPS, FRIENDLIES, ETC -- general for all agencies
 -- =======================
 CREATE TABLE competitions (
     id BIGSERIAL PRIMARY KEY,
     name TEXT NOT NULL,
     short_name TEXT,
     type VARCHAR(50) NOT NULL, -- DOMESTIC_LEAGUE, INTERNATIONAL_CUP, FRIENDLY, ETC
     country_id BIGINT REFERENCES countries(id),
     confederation TEXT, -- e.g., UEFA, CONMEBOL, CAF, AFC, CONCACAF, OFC, etc
     tier INTEGER CHECK (tier > 0),
     is_active BOOLEAN DEFAULT TRUE,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 );

 -- =======================
 -- SEASONS -- COMPETITION SEASONS i.e. 2023/2024 -- general for all agencies
 -- =======================
 CREATE TABLE seasons (
     id BIGSERIAL PRIMARY KEY,
     competition_id BIGINT NOT NULL REFERENCES competitions(id) ON DELETE CASCADE,
     name TEXT NOT NULL,
     start_date DATE,
     end_date DATE,
     is_current BOOLEAN DEFAULT FALSE,
     CHECK (start_date IS NULL OR end_date IS NULL OR start_date < end_date)
 );

 CREATE UNIQUE INDEX uq_current_season
 ON seasons (competition_id)
 WHERE is_current = TRUE;

 -- =======================
 -- TEAMS -- general for all agencies
 -- =======================
 CREATE TABLE teams (
     id BIGSERIAL PRIMARY KEY,
     name TEXT NOT NULL,
     short_name TEXT,
     country_id BIGINT,
     team_type team_type NOT NULL,
     team_logo_url TEXT,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     CONSTRAINT uq_team_name_team_type UNIQUE (name, team_type),
     CONSTRAINT fk_team_country FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE SET NULL
 );

 -- =======================
 -- MATCHES (NO SCORES) -- general for all agencies
 -- =======================
 CREATE TABLE matches (
     id BIGSERIAL PRIMARY KEY,
     season_id BIGINT,
     competition_id BIGINT,
     home_team_id BIGINT NOT NULL,
     away_team_id BIGINT NOT NULL,
     kickoff_time TIMESTAMP NOT NULL,
     status match_status DEFAULT 'SCHEDULED',
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     CONSTRAINT uq_match CHECK (home_team_id <> away_team_id),
     CONSTRAINT fk_match_competition FOREIGN KEY (competition_id) REFERENCES competitions(id) ON DELETE SET NULL,
     CONSTRAINT fk_match_season FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE SET NULL,
     CONSTRAINT fk_match_home_team FOREIGN KEY (home_team_id) REFERENCES teams(id) ON DELETE CASCADE,
     CONSTRAINT fk_match_away_team FOREIGN KEY (away_team_id) REFERENCES teams(id) ON DELETE CASCADE

 );

 CREATE INDEX idx_matches_kickoff ON matches(kickoff_time);
 CREATE INDEX idx_matches_status ON matches(status);

 -- =======================
 -- MATCH RESULTS (SOURCE OF TRUTH) -- general for all agencies
 -- =======================
 CREATE TABLE match_results (
     id BIGSERIAL PRIMARY KEY,
     match_id BIGINT NOT NULL UNIQUE,

     ht_home_goals INT CHECK (ht_home_goals >= 0),
     ht_away_goals INT CHECK (ht_away_goals >= 0),
     ft_home_goals INT CHECK (ft_home_goals >= 0),
     ft_away_goals INT CHECK (ft_away_goals >= 0),
     et_home_goals INT CHECK (et_home_goals >= 0),
     et_away_goals INT CHECK (et_away_goals >= 0),
     penalties_home INT CHECK (penalties_home >= 0),
     penalties_away INT CHECK (penalties_away >= 0),

     home_shots INT CHECK (home_shots >= 0),
     away_shots INT CHECK (away_shots >= 0),
     home_shots_on_target INT CHECK (home_shots_on_target >= 0),
     away_shots_on_target INT CHECK (away_shots_on_target >= 0),

     home_yellow_cards INT CHECK (home_yellow_cards >= 0),
     away_yellow_cards INT CHECK (away_yellow_cards >= 0),
     home_red_cards INT CHECK (home_red_cards >= 0),
     away_red_cards INT CHECK (away_red_cards >= 0),

     home_corners INT CHECK (home_corners >= 0),
     away_corners INT CHECK (away_corners >= 0),

     home_possession NUMERIC(5,2),
     away_possession NUMERIC(5,2),

     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     CONSTRAINT fk_match_result_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
     CONSTRAINT check_possession CHECK (
              home_possession IS NULL OR
              away_possession IS NULL OR
              home_possession + away_possession BETWEEN 99.9 AND 100.1
          ),
 );

 -- =======================
 -- BET MARKETS -- general for all agencies
 -- =======================
 CREATE TABLE bet_markets (
     id BIGSERIAL PRIMARY KEY,
     name VARCHAR(50) NOT NULL UNIQUE,
     description TEXT,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 );

 -- =======================
 -- BET OPTIONS -- general for all agencies
 -- =======================
 CREATE TABLE bet_options (
     id BIGSERIAL PRIMARY KEY,
     market_id BIGINT NOT NULL REFERENCES bet_markets(id) ON DELETE CASCADE,
     option_name VARCHAR(50) NOT NULL,
     option_value TEXT,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     UNIQUE (market_id, option_name)
 );

 CREATE INDEX idx_bet_options_market ON bet_options(market_id);

 -- =======================
 -- MATCH BETS (ODDS) -- general for all agencies
 -- =======================
 CREATE TABLE match_bets (
     id BIGSERIAL PRIMARY KEY,
     match_id BIGINT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
     bet_option_id BIGINT NOT NULL REFERENCES bet_options(id) ON DELETE CASCADE,
     odds NUMERIC(6,2) NOT NULL CHECK (odds > 1),
     status bet_status DEFAULT 'PENDING',
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     UNIQUE (match_id, bet_option_id)
 );

 CREATE INDEX idx_match_bets_match ON match_bets(match_id);

 -- =======================
 -- USER BETS -- Specific to each agency
 -- =======================
 CREATE TABLE bets (
     id BIGSERIAL PRIMARY KEY,
     user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     agent_id BIGINT NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
     total_stake NUMERIC(12,2) NOT NULL CHECK (total_stake > 0),
     potential_payout NUMERIC(12,2),
     payout_type payout_type NOT NULL DEFAULT 'ODD_BASED',
     type bet_type NOT NULL DEFAULT 'SINGLE', -- SINGLE, MULTIPLE
     status bet_status DEFAULT 'PENDING',
     placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     settled_at TIMESTAMP,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 );

 CREATE INDEX idx_bets_user_status ON bets(user_id, status);

 -- =======================
 -- BET SELECTIONS
 -- =======================
 CREATE TABLE bet_selections (
     id BIGSERIAL PRIMARY KEY,
     bet_id BIGINT NOT NULL REFERENCES bets(id) ON DELETE CASCADE,
     match_bet_id BIGINT NOT NULL REFERENCES match_bets(id) ON DELETE CASCADE,
     agent_id BIGINT NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
     stake NUMERIC(12,2) NOT NULL CHECK (stake > 0),
     selected_odds NUMERIC(6,2) CHECK (selected_odds > 1),
     status bet_status DEFAULT 'PENDING',
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     UNIQUE (bet_id, match_bet_id)
 );

========================= SPOT THE BALL DESIGN ===========================

-- =======================
-- WINNING ZONES -- GENERAL
-- =======================
CREATE TABLE spot_winning_zones (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    zone_name TEXT NOT NULL,
    zone_order INT NOT NULL,           -- 1 = closest to ball
    max_radius_px INT NOT NULL,        -- max distance in pixels
    is_active BOOLEAN DEFAULT FALSE,
    description TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CHECK (max_radius_px > 0),
    UNIQUE (zone_order, agent_id)
);

-- =======================
-- SPOT THE BALL GAMES
-- =======================
CREATE TABLE spot_the_ball (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    image_url TEXT NOT NULL,
    image_width INT NOT NULL,
    image_height INT NOT NULL,

    -- secret position (hashed for provably fair)
    secret_hash TEXT NOT NULL,
    secret_salt TEXT NOT NULL,
    hash_algorithm TEXT DEFAULT 'SHA256',

    -- revealed later
    ball_x NUMERIC,  -- subpixel accuracy possible
    ball_y NUMERIC,

    revealed_at TIMESTAMP,
    last_status_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CHECK (opens_at < closes_at),
    CHECK (ball_x IS NULL OR ball_x BETWEEN 0 AND image_width),
    CHECK (ball_y IS NULL OR ball_y BETWEEN 0 AND image_height)
);

-- Indexes for performance
CREATE INDEX idx_spot_the_ball_status ON spot_the_ball(status);
CREATE INDEX idx_spot_the_ball_open_games ON spot_the_ball(status, opens_at);

-- =======================
-- SPOT THE BALL COMPETITIONS
-- =======================
CREATE TABLE spot_competitions (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    spot_id BIGINT NOT NULL REFERENCES spot_the_ball(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,

    ticket_price NUMERIC(10,2) NOT NULL,
    current_entries INT NOT NULL DEFAULT 0,
    total_entries_pool INT NOT NULL DEFAULT 0,
    minimum_entries_for_prize INT NOT NULL DEFAULT 0, -- minimum entries required to trigger prize payout

    status VARCHAR(50) DEFAULT 'SCHEDULED', -- 'SCHEDULED', 'OPEN', 'CLOSED', 'REVEALED', 'PAID', 'CANCELLED'
    description TEXT, -- describe anything about the game

    min_entries INT NOT NULL DEFAULT 0, -- minimum entries a player must make to join
    max_entries INT NOT NULL DEFAULT 0, -- maximum entries a player can make
    is_active BOOLEAN DEFAULT FALSE,
    image_url TEXT NOT NULL,
    image_width INT NOT NULL CHECK (image_width > 0),
    image_height INT NOT NULL CHECK (image_height > 0),

    opens_at TIMESTAMP NOT NULL,
    closes_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE competition_zone_pool (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL,
    zone_id BIGINT NOT NULL,
    prize_amount NUMERIC(12,2) NOT NULL DEFAULT 0.0,
    prize_name VARCHAR(255),
    prize_type VARCHAR(50) NOT NULL, -- 'CASH', 'IN_KIND', etc
    description TEXT, -- e.g "Ticker value return"

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (competition_id, zone_id),
    CONSTRAINT fk_competition_zone_pool_competition FOREIGN KEY (competition_id) REFERENCES spot_competitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_competition_zone_pool_zone FOREIGN KEY (zone_id) REFERENCES spot_winning_zones(id) ON DELETE CASCADE
);

-- =======================
-- PLAYER ENTRIES / GUESSES
-- =======================
CREATE TABLE competition_entries (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    competition_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    guess_x NUMERIC NOT NULL,
    guess_y NUMERIC NOT NULL,

    distance_px NUMERIC(10,2),      -- calculated after reveal
    zone_id BIGINT, -- calculated after reveal

    stake NUMERIC(10,2) NOT NULL CHECK (stake > 0),
    payout NUMERIC(12,2),            -- calculated after reveal

    entry_order BIGINT,              -- optional for fairness
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CHECK (guess_x >= 0),
    CHECK (guess_y >= 0),
    CONSTRAINT fk_competition_entries_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    CONSTRAINT fk_competition_entries_competition FOREIGN KEY (competition_id) REFERENCES spot_competitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_competition_entries_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_competition_entries_zone FOREIGN KEY (zone_id) REFERENCES spot_winning_zones(id) ON DELETE SET NULL
);

CREATE INDEX idx_spot_entries_game ON spot_entries(game_id);
CREATE INDEX idx_spot_entries_user ON spot_entries(user_id);
CREATE INDEX idx_spot_entries_game_zone ON spot_entries(game_id, zone_id);

-- =======================
-- ZONE-LEVEL PAYOUTS
-- =======================
CREATE TABLE spot_prize_payouts (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    entry_id BIGINT NOT NULL,
    competition_id BIGINT NOT NULL,
    zone_id BIGINT NOT NULL,
    amount NUMERIC(12,2) NOT NULL,

    settled_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_spot_payouts_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    CONSTRAINT fk_spot_payouts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_spot_payouts_entry FOREIGN KEY (entry_id) REFERENCES competition_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_spot_payouts_competition FOREIGN KEY (competition_id) REFERENCES spot_competitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_spot_payouts_zone FOREIGN KEY (zone_id) REFERENCES spot_winning_zones(id) ON DELETE CASCADE
);

CREATE INDEX idx_spot_payouts_game_zone ON spot_payouts(game_id, zone_id);


