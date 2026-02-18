-- =====================================================
-- Flyway Migration: V1
-- Description: Create customers table
-- Author: Medo Team
-- =====================================================
-- PURPOSE:
--   Creates the customers table to store customer information.
--
-- TABLE STRUCTURE:
--   - customer_id: Primary key (auto-increment)
--   - name: Customer name (required, max 100 chars)
--   - email: Customer email (required, max 100 chars)
--   - mobile_number: Customer mobile number (required, unique, 10 digits)
--   - created_at: Audit field - record creation timestamp
--   - created_by: Audit field - who created the record
--   - updated_at: Audit field - last update timestamp
--   - updated_by: Audit field - who last updated the record
-- =====================================================

CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    mobile_number VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    INDEX idx_mobile_number (mobile_number),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comment to table
ALTER TABLE customers COMMENT = 'Customer information - Stores basic customer details';
