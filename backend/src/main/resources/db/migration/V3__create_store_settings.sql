-- Store Hours
CREATE TABLE store_hours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    opening_time TIME,
    closing_time TIME,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(store_id, day_of_week)
);

CREATE INDEX idx_store_hours_store_id ON store_hours(store_id);

-- Store Holidays
CREATE TABLE store_holidays (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    holiday_date DATE NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_store_holidays_store_id ON store_holidays(store_id);

-- Delivery Settings
CREATE TABLE delivery_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE REFERENCES stores(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_radius DOUBLE PRECISION CHECK (delivery_radius IS NULL OR delivery_radius >= 0),
    delivery_fee DECIMAL(10,2) CHECK (delivery_fee IS NULL OR delivery_fee >= 0),
    minimum_order DECIMAL(10,2) CHECK (minimum_order IS NULL OR minimum_order >= 0),
    free_delivery_threshold DECIMAL(10,2) CHECK (free_delivery_threshold IS NULL OR free_delivery_threshold >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Payment Preferences
CREATE TABLE payment_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    payment_method VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(store_id, payment_method)
);

CREATE INDEX idx_payment_preferences_store_id ON payment_preferences(store_id);
