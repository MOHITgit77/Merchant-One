-- Add version column for optimistic locking
ALTER TABLE product_variants ADD COLUMN version INTEGER DEFAULT 0 NOT NULL;
