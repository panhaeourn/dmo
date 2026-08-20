ALTER TABLE cito_receipts
    ADD COLUMN IF NOT EXISTS birth_date VARCHAR(255);
