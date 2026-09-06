-- US-02.3: Add display_order and is_active columns to categories table
ALTER TABLE categories ADD COLUMN display_order INT NOT NULL DEFAULT 0;
ALTER TABLE categories ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
