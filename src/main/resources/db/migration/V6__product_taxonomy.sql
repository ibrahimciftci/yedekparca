CREATE TABLE product_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uk_product_categories_name_ci ON product_categories (LOWER(name));

CREATE TABLE product_brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uk_product_brands_name_ci ON product_brands (LOWER(name));

INSERT INTO product_categories (name)
SELECT DISTINCT BTRIM(category)
FROM catalog_products
WHERE category IS NOT NULL AND BTRIM(category) <> ''
ON CONFLICT (LOWER(name)) DO NOTHING;

INSERT INTO product_brands (name)
SELECT DISTINCT BTRIM(brand)
FROM catalog_products
WHERE brand IS NOT NULL AND BTRIM(brand) <> ''
ON CONFLICT (LOWER(name)) DO NOTHING;

INSERT INTO product_categories (name) VALUES
('Motor Parçaları'), ('Kaporta'), ('Elektrik & Elektronik'), ('Araç Aksesuarları'),
('Fren Sistemi'), ('Süspansiyon ve Direksiyon'), ('Şanzıman ve Debriyaj'),
('Soğutma ve Isıtma'), ('Yakıt ve Egzoz Sistemi'), ('Filtreler ve Yağlar'),
('Aydınlatma'), ('Jant ve Lastik'), ('İç Donanım'), ('Dış Donanım'), ('Diğer')
ON CONFLICT (LOWER(name)) DO NOTHING;

INSERT INTO product_brands (name) VALUES
('Bosch'), ('Valeo'), ('Mahle'), ('Mann-Filter'), ('Schaeffler'), ('LuK'), ('INA'), ('FAG'),
('ZF'), ('Brembo'), ('TRW'), ('Febi Bilstein'), ('Lemförder'), ('Sachs'), ('KYB'),
('NGK'), ('Denso'), ('Continental'), ('Hella'), ('Delphi'), ('Pierburg'), ('Gates'),
('Dayco'), ('Meyle'), ('Moog'), ('Monroe'), ('Bilstein'), ('Osram'), ('Philips'),
('Castrol'), ('Mobil'), ('Shell'), ('Motul'), ('TotalEnergies'), ('Diğer')
ON CONFLICT (LOWER(name)) DO NOTHING;
