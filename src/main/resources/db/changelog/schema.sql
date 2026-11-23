--liquibase formatted sql
--changeset student:001-create-schema
CREATE TABLE clients (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE accounts (
                          id BIGSERIAL PRIMARY KEY,
                          client_id BIGINT NOT NULL,
                          account_number VARCHAR(34) NOT NULL,
                          balance NUMERIC(19,4) NOT NULL DEFAULT 0,
                          CONSTRAINT fk_accounts_client
                              FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              account_id BIGINT NOT NULL,
                              amount NUMERIC(19,4) NOT NULL,
                              type VARCHAR(20) NOT NULL,
                              transaction_date TIMESTAMP NOT NULL DEFAULT NOW(),
                              CONSTRAINT fk_transactions_account
                                  FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_accounts_client_id
    ON accounts(client_id);

CREATE INDEX idx_transactions_account_date
    ON transactions(account_id, transaction_date);