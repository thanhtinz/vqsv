-- V7__payment_settings.sql
-- Payment configuration is stored in the DB and edited from the admin panel
-- (nothing in code / env). Single-row table for the SePay bank-transfer gateway.

CREATE TABLE payment_settings (
    id             SMALLINT     PRIMARY KEY DEFAULT 1,
    enabled        BOOLEAN      NOT NULL DEFAULT FALSE,
    sepay_api_key  VARCHAR(128) NOT NULL DEFAULT '',  -- shared secret SePay sends in the webhook
    bank_account   VARCHAR(32)  NOT NULL DEFAULT '',  -- số tài khoản nhận tiền
    bank_code      VARCHAR(32)  NOT NULL DEFAULT '',  -- mã ngân hàng (VietQR), vd MBBank, VCB
    account_holder VARCHAR(64)  NOT NULL DEFAULT '',  -- tên chủ tài khoản
    prefix         VARCHAR(16)  NOT NULL DEFAULT 'VQSV', -- tiền tố nội dung chuyển khoản
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT payment_settings_singleton CHECK (id = 1)
);

INSERT INTO payment_settings (id) VALUES (1);
