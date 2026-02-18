-- =====================================================
-- Flyway Migration: V2
-- Description: Create accounts table
-- Author: Medo Team
-- =====================================================
-- PURPOSE:
--   Creates the accounts table to store bank account information.
--
-- TABLE STRUCTURE:
--   - account_number: Primary key (unique account identifier)
--   - customer_id: Foreign key to customers table
--   - account_type: Type of account (Savings, Current, etc.)
--   - branch_address: Bank branch address
--   - created_at: Audit field - record creation timestamp
--   - created_by: Audit field - who created the record
--   - updated_at: Audit field - last update timestamp
--   - updated_by: Audit field - who last updated the record
--
-- CONSTRAINTS:
--   - Foreign key ensures referential integrity with customers table
--   - ON DELETE CASCADE ensures accounts are deleted when customer is deleted
-- =====================================================

CREATE TABLE IF NOT EXISTS accounts (
    account_number BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    branch_address VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    CONSTRAINT fk_accounts_customer_id
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
        ON DELETE CASCADE,
    INDEX idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comment to table
ALTER TABLE accounts COMMENT = 'Bank accounts - Stores customer bank account details';

-- Add check constraint for account_type
ALTER TABLE accounts
ADD CONSTRAINT chk_account_type
CHECK (account_type IN ('Savings', 'Current', 'Salary', 'Fixed Deposit', 'Recurring Deposit'));
