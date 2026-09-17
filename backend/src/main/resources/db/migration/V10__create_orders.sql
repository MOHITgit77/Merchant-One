-- V10: Order Management
-- Orders placed by customers, managed by merchants

CREATE TABLE orders (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    store_id        UUID NOT NULL REFERENCES stores(id),
    order_number    VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    order_type      VARCHAR(10) NOT NULL DEFAULT 'PICKUP',

    -- Customer info
    customer_name   VARCHAR(100) NOT NULL,
    customer_phone  VARCHAR(20) NOT NULL,
    customer_email  VARCHAR(255),

    -- Delivery info
    delivery_address TEXT,

    -- Payment
    payment_method  VARCHAR(20) NOT NULL,

    -- Amounts
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount    NUMERIC(12,2) NOT NULL DEFAULT 0,

    -- Notes
    notes           TEXT,
    merchant_notes  TEXT,
    cancellation_reason TEXT,

    -- Idempotency
    idempotency_key VARCHAR(100),

    -- Timestamps
    accepted_at     TIMESTAMPTZ,
    rejected_at     TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    cancelled_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_orders_order_number UNIQUE (store_id, order_number),
    CONSTRAINT uq_orders_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING','ACCEPTED','PREPARING','READY','COMPLETED','REJECTED','CANCELLED')),
    CONSTRAINT chk_orders_type CHECK (order_type IN ('PICKUP','DELIVERY'))
);

CREATE TABLE order_items (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id      UUID NOT NULL REFERENCES product_variants(id),

    -- Snapshot of product info at time of order
    product_name    VARCHAR(255) NOT NULL,
    variant_name    VARCHAR(255),
    sku             VARCHAR(100),

    quantity        INT NOT NULL,
    unit_price      NUMERIC(12,2) NOT NULL,
    tax_percent     NUMERIC(5,2) NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(12,2) NOT NULL DEFAULT 0,
    line_total      NUMERIC(12,2) NOT NULL,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for performance
CREATE INDEX idx_orders_store_id ON orders(store_id);
CREATE INDEX idx_orders_status ON orders(store_id, status);
CREATE INDEX idx_orders_created_at ON orders(store_id, created_at DESC);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_orders_idempotency ON orders(idempotency_key);
