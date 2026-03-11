CREATE TABLE agents (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ACCESS_KEYS (
    id BIGSERIAL PRIMARY KEY,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    agent_id BIGINT NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--=================================================================

CREATE TABLE user_profile (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT NOT NULL UNIQUE,  -- Telegram ID is unique and immutable
    referrer_id BIGINT,  -- Self-referential foreign key
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50),
    nickname VARCHAR(100),
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'BANNED'
    role VARCHAR(30) NOT NULL DEFAULT 'PLAYER',  -- 'PLAYER', 'MODERATOR', 'ADMIN'
    is_deleted BOOLEAN DEFAULT FALSE,
    is_bot BOOLEAN DEFAULT FALSE,
    bot_room_id BIGINT,
    password TEXT, -- for bank withdrawal authentication
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for user_profile
CREATE INDEX idx_user_profile_status ON user_profile(status);
CREATE INDEX idx_user_profile_role ON user_profile(role);


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
    -- new
    min_draws INT DEFAULT 6,
    max_draws INT DEFAULT 10,
    fake_winner_id BIGINT,
    fake_winner_telegram_id BIGINT,
    fake_win_enabled BOOLEAN DEFAULT FALSE,
    --
    commission_rate NUMERIC(5, 2) DEFAULT 0.2,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_created_by FOREIGN KEY (created_by) REFERENCES user_profile(id) ON DELETE SET NULL
);

-- Indexes for room
CREATE INDEX idx_room_status ON room(status);
CREATE INDEX idx_room_pattern ON room(pattern);
CREATE INDEX idx_room_created_by ON room(created_by);


CREATE TABLE game (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT,
    joined_players_ids TEXT,
    drawn_numbers TEXT,
--    all_card_ids TEXT,
    players_count INT NOT NULL DEFAULT 0,
    entries_count INT NOT NULL DEFAULT 0,
    prize_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    commission_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    capacity INT NOT NULL,
    entry_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.0,

    started BOOLEAN NOT NULL DEFAULT FALSE,
    ended BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'READY', -- 'READY', 'PLAYING', 'COMPLETED', 'CANCELLED'
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT game_state_room FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE SET NULL
);

-- Indexes for game
CREATE INDEX idx_game_room_id ON game(room_id);
CREATE INDEX idx_game_status ON game(status);


CREATE TABLE game_transaction (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT,
    player_id BIGINT,
    txn_amount NUMERIC(19, 2) NOT NULL, -- total amount involved in the transaction
    single_game_fee NUMERIC(19, 2) DEFAULT 0.0, -- for GAME_FEE type transactions, single game fee
    commission_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.0,
    txn_type VARCHAR(50) NOT NULL,  -- e.g., GAME_FEE, PRIZE_PAYOUT, REFUND, DISPUTE
    txn_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- e.g., SUCCESS, FAIL, AWAITING_APPROVAL
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_game_transaction_game FOREIGN KEY (game_id)
        REFERENCES game(id) ON DELETE SET NULL,
    CONSTRAINT fk_game_transaction_user FOREIGN KEY (player_id)
        REFERENCES user_profile(id) ON DELETE SET NULL
);

-- Useful indexes
CREATE INDEX idx_game_transaction_game_id ON game_transaction(game_id);
CREATE INDEX idx_game_transaction_player ON game_transaction(player_id);
CREATE INDEX idx_game_transaction_status ON game_transaction(txn_status);
CREATE INDEX idx_game_transaction_type ON game_transaction(txn_type);


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
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bingo_claims_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE,
    CONSTRAINT fk_bingo_claims_user_profile FOREIGN KEY (player_id) REFERENCES user_profile(id) ON DELETE CASCADE
);

-- Indexes for bingo_claims
CREATE INDEX idx_bingo_claims_game_id ON bingo_claims(game_id);
CREATE INDEX idx_bingo_claims_player_id ON bingo_claims(player_id);

-- transaction_status AS ENUM ('PENDING', 'AWAITING_APPROVAL', 'COMPLETED', 'FAILED');

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
CREATE UNIQUE INDEX idx_default_payment_method
ON payment_method (is_default)
WHERE is_default = true;

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
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_user FOREIGN KEY (user_id)
        REFERENCES user_profile(id) ON DELETE CASCADE,

    CONSTRAINT fk_payment_method FOREIGN KEY (payment_method_id)
        REFERENCES payment_method(id) ON DELETE CASCADE
);

-- ===============================
-- INDEXES FOR PERFORMANCE
-- ===============================

-- Fast lookup by user (user’s orders)
CREATE INDEX IF NOT EXISTS idx_payment_order_user_id
    ON payment_order (user_id);

-- Fast lookup by transaction reference (for reconciliation / provider callbacks)
CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_order_txn_ref
    ON payment_order (txn_ref);

-- Fast lookup by provider reference (AddisPay or other external integrations)
CREATE INDEX IF NOT EXISTS idx_payment_order_provider_ref
    ON payment_order (provider_order_ref);

-- Query filtering or pagination by status
CREATE INDEX IF NOT EXISTS idx_payment_order_status
    ON payment_order (status);

-- Query by payment method (useful for admin dashboards)
CREATE INDEX IF NOT EXISTS idx_payment_order_payment_method
    ON payment_order (payment_method_id);

-- Queries filtering by transaction type (Deposit, Withdrawal)
CREATE INDEX IF NOT EXISTS idx_payment_order_txn_type
    ON payment_order (txn_type);

-- Ordering and pagination by created_at (common default sort)
CREATE INDEX IF NOT EXISTS idx_payment_order_created_at
    ON payment_order (created_at DESC);

-- Ordering and pagination by updated_at (for “recently updated” listings)
CREATE INDEX IF NOT EXISTS idx_payment_order_updated_at
    ON payment_order (updated_at DESC);

-- For admin approval queries (offline orders awaiting approval)
CREATE INDEX IF NOT EXISTS idx_payment_order_approved_by
    ON payment_order (approved_by);



-- ===============================
-- TRANSACTION TABLE DEFINITION
-- ===============================

CREATE TABLE IF NOT EXISTS transaction (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    txn_ref VARCHAR(50) UNIQUE NOT NULL,
    payment_method_id BIGINT NOT NULL,
    txn_type VARCHAR(50) NOT NULL,
    txn_amount NUMERIC(18, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    meta_data TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_transaction_player FOREIGN KEY (player_id)
        REFERENCES user_profile(id) ON DELETE CASCADE,

    CONSTRAINT fk_transaction_order FOREIGN KEY (order_id)
        REFERENCES payment_order(id) ON DELETE CASCADE,

    CONSTRAINT fk_transaction_payment_method FOREIGN KEY (payment_method_id)
        REFERENCES payment_method(id) ON DELETE CASCADE
);

-- ===============================
-- INDEXES FOR PERFORMANCE
-- ===============================

-- Fast lookup by player for transaction history
CREATE INDEX IF NOT EXISTS idx_transaction_player_id
    ON transaction (player_id);

-- Fast lookup by order linkage
CREATE INDEX IF NOT EXISTS idx_transaction_order_id
    ON transaction (order_id);

-- Fast lookup by txn_ref for reconciliation (must be unique)
CREATE UNIQUE INDEX IF NOT EXISTS idx_transaction_txn_ref
    ON transaction (txn_ref);

-- Queries filtered by status (e.g., COMPLETED, FAILED)
CREATE INDEX IF NOT EXISTS idx_transaction_status
    ON transaction (status);

-- Queries filtered by type (e.g., DEPOSIT, WITHDRAWAL)
CREATE INDEX IF NOT EXISTS idx_transaction_txn_type
    ON transaction (txn_type);

-- Fast lookup by payment method
CREATE INDEX IF NOT EXISTS idx_transaction_payment_method
    ON transaction (payment_method_id);

-- Sorting and pagination
CREATE INDEX IF NOT EXISTS idx_transaction_created_at
    ON transaction (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transaction_updated_at
    ON transaction (updated_at DESC);

-- Composite index for admin dashboards (status + type + date)
CREATE INDEX IF NOT EXISTS idx_transaction_status_type_created_at
    ON transaction (status, txn_type, created_at DESC);


--TODO: Make sure transfer is made between two uses in same agency - in backend
CREATE TABLE deposit_transfer (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- SUCCESS, FAIL, AWAITING_APPROVAL
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deposit_transfer_sender FOREIGN KEY (sender_id)
        REFERENCES user_profile(id) ON DELETE RESTRICT,
    CONSTRAINT fk_deposit_transfer_receiver FOREIGN KEY (receiver_id)
        REFERENCES user_profile(id) ON DELETE RESTRICT
);

CREATE INDEX idx_sender_id ON deposit_transfer(sender_id);
CREATE INDEX idx_receiver_id ON deposit_transfer(receiver_id);


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
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
);

CREATE UNIQUE INDEX idx_wallet_user_profile_id ON wallet(user_profile_id);
CREATE INDEX idx_wallet_user_profile_id ON wallet(user_profile_id);

-- system_config Table
CREATE TABLE system_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    value TEXT NOT NULL,
    possible_values TEXT, -- comma separated possible values -- NEW
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_system_config_name UNIQUE (name)
);

-- Indexes for system_config
CREATE INDEX idx_system_config_name ON system_config(name);


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

=========================== NEW ============================

CREATE TABLE daily_accounting (
    id BIGSERIAL PRIMARY KEY,
    accounting_date DATE NOT NULL UNIQUE,
    daily_deposit_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_withdrawal_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_bet_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_prize_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_commission_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_promotional_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_welcome_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE total_accounting (
    id BIGSERIAL PRIMARY KEY,
    total_deposit_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_withdrawal_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_bet_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_prize_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_commission_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_promotional_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_welcome_bonus_amount NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

============================================================

CREATE TABLE referral_history (
    id BIGSERIAL PRIMARY KEY,
    referrer_id BIGINT NOT NULL,
    referee_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_referrer_not_same_as_referee
        CHECK (referrer_id <> referee_id)
);

CREATE INDEX idx_referrer_id ON referral_history(referrer_id);
CREATE INDEX idx_referee_id ON referral_history(referee_id);


=========================== LEADERBOARD ============================
CREATE TABLE daily_leaderboard (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_profile(id),
    daily_games_played INT DEFAULT 0 NOT NULL,
    daily_wins INT DEFAULT 0 NOT NULL,
    daily_prize NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_bets NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_deposit NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    daily_withdrawal NUMERIC(19, 2) DEFAULT 0.00 NOT NULL, -- NEW
    leaderboard_date DATE NOT NULL,
    is_bot BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_daily_leaderboard_user_date UNIQUE (user_id, leaderboard_date)
);


CREATE TABLE total_leaderboard (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_profile(id),
    total_games_played INT DEFAULT 0 NOT NULL,
    total_wins INT DEFAULT 0 NOT NULL,
    total_prize NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_bets NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_deposit NUMERIC(19, 2) DEFAULT 0.00 NOT NULL,
    total_withdrawal NUMERIC(19, 2) DEFAULT 0.00 NOT NULL, -- NEW
    is_bot BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_total_leaderboard_user UNIQUE (user_id)
);


--========================= Guess football design ============================
-- MATCHES
CREATE TABLE country (
    id  BIGSERIAL PRIMARY KEY,)
    country_name    VARCHAR(100) NOT NULL,
    country_code    VARCHAR(10),
    country_flag_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
);

Create TABLE league (
    id BIGSERIAL PRIMARY KEY,
    league_name VARCHAR(100) NOT NULL,
    league_country_id BIGINT NOT NULL REFERENCES country(country_id),
    league_logo_url TEXT,
    is_same_country BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
);

CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    team_name VARCHAR(100) NOT NULL,
    team_country_id BIGINT NOT NULL REFERENCES country(country_id),
    team_logo_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
);

CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,
    league_id BIGINT NOT NULL REFERENCES league(league_id),
    home_team_id BIGINT NOT NULL REFERENCES teams(team_id),
    away_team_id BIGINT NOT NULL REFERENCES teams(team_id),
    start_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,  -- SCHEDULED, LIVE, FINISHED, POSTPONED, CANCELLED

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
);


CREATE TABLE winning_prize_mode (
    id BIGSERIAL PRIMARY KEY,
    mode_name VARCHAR(100) NOT NULL, -- SHARED, RANKED
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);


CREATE TABLE bet_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL, -- ft_home_win, ft_away_win, ht_home_win, ft_draw, total_goals_over_under, first_goal_team, etc
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);


-- -- types
--    ft_home_goals    INT,
--    ft_away_goals    INT,
--    ht_home_goals    INT,
--    ht_away_goals    INT,
--    first_goal_time_range  INT,
--    total_goals      INT,
--    total_yellow_cards INT,
--    total_red_cards  INT,
--    total_corners    INT,
--    total_shots      INT,
--    total_shots_on_target INT,
--    total_fouls      INT,
--    total_offsides   INT,
--    winner_possession NUMERIC(5,2),  -- percentage
--    loser_possession  NUMERIC(5,2),  -- percentage


CREATE TABLE match_bets (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL REFERENCES matches(match_id),
    bet_type_id BIGINT NOT NULL REFERENCES bet_types(id),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(match_id, bet_type_id)
);

-- OPTIONAL: First goal time ranges
CREATE TABLE goal_time_ranges (
    id BIGSERIAL PRIMARY KEY,
    min_time INT NOT NULL,
    max_time INT NOT NULL,
    description VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- PREDICTIONS
CREATE TABLE match_bet_predictions (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL REFERENCES matches(match_id),
    user_id  BIGINT NOT NULL REFERENCES user_profile(user_id),

    -- Full-time score
    ft_home_goals    INT,
    ft_away_goals    INT,
    ht_home_goals    INT,
    ht_away_goals    INT,
    first_goal_time_code  INT,           -- minute of first goal
    first_goal_team  VARCHAR(50),   -- HOME / AWAY / NONE
    created_at            TIMESTAMPTZ DEFAULT now(),

    UNIQUE(user_id, match_id)
);

-- INDEXES
CREATE INDEX idx_pred_match ON match_predictions(match_id);
CREATE INDEX idx_pred_user  ON match_predictions(user_id);

CREATE TABLE prediction_payment(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_profile(user_id),
    match_id BIGINT NOT NULL REFERENCES matches(match_id),
    amount NUMERIC(12,2) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,  -- PENDING, COMPLETED, FAILED
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_id, match_id)
    );

CREATE TABLE prediction_setting (
    id BIGSERIAL PRIMARY KEY,
    entry_fee NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    prize_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);
