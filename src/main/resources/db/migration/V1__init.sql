CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE dialogs (
    id BIGSERIAL PRIMARY KEY,
    telegram_chat_id BIGINT NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    last_message_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    dialog_id BIGINT NOT NULL REFERENCES dialogs(id),
    sender_id BIGINT REFERENCES users(id),
    text TEXT NOT NULL,
    telegram_message_id BIGINT NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE mcp_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_dialog_sent_at ON messages(dialog_id, sent_at);
CREATE INDEX idx_messages_telegram_id ON messages(telegram_message_id);
CREATE INDEX idx_dialogs_telegram_chat_id ON dialogs(telegram_chat_id);
