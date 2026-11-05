ALTER TABLE product_offers ADD COLUMN IF NOT EXISTS offer_description VARCHAR(4000);
UPDATE product_offers o SET offer_description=p.description FROM products p WHERE o.legacy_product_id=p.id AND o.offer_description IS NULL;
