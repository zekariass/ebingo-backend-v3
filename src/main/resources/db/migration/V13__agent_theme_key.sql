-- Per-agent UI theme key. NULL = built-in "default" palette on the client.
ALTER TABLE agents
    ADD COLUMN theme_key VARCHAR(64) NULL;
