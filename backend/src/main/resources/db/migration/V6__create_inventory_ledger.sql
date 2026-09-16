CREATE TABLE inventory_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id),
    variant_id UUID NOT NULL REFERENCES product_variants(id),
    movement_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    quantity_before INTEGER NOT NULL,
    quantity_after INTEGER NOT NULL,
    unit_cost DECIMAL(10,2),
    reference_type VARCHAR(30),
    reference_id UUID,
    notes TEXT,
    performed_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_variant_id ON inventory_ledger(variant_id);
CREATE INDEX idx_ledger_store_id ON inventory_ledger(store_id);
CREATE INDEX idx_ledger_created_at ON inventory_ledger(created_at);
CREATE INDEX idx_ledger_movement_type ON inventory_ledger(store_id, movement_type);
