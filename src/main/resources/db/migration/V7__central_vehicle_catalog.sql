CREATE TABLE vehicle_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE vehicle_brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    normalized_name VARCHAR(150) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE vehicle_models (
    id BIGSERIAL PRIMARY KEY,
    brand_id BIGINT NOT NULL REFERENCES vehicle_brands(id),
    name VARCHAR(180) NOT NULL,
    normalized_name VARCHAR(180) NOT NULL,
    source_countries VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_vehicle_model_brand_name UNIQUE (brand_id, normalized_name)
);

CREATE TABLE vehicle_model_types (
    vehicle_model_id BIGINT NOT NULL REFERENCES vehicle_models(id) ON DELETE CASCADE,
    vehicle_type_id BIGINT NOT NULL REFERENCES vehicle_types(id),
    PRIMARY KEY (vehicle_model_id, vehicle_type_id)
);

CREATE INDEX idx_vehicle_models_brand_name ON vehicle_models (brand_id, normalized_name);
CREATE INDEX idx_vehicle_model_types_type ON vehicle_model_types (vehicle_type_id, vehicle_model_id);
CREATE INDEX idx_vehicle_brands_normalized ON vehicle_brands (normalized_name);

ALTER TABLE product_vehicle_compatibilities
    ADD COLUMN vehicle_model_id BIGINT REFERENCES vehicle_models(id);

CREATE INDEX idx_vehicle_compat_model ON product_vehicle_compatibilities (vehicle_model_id);
