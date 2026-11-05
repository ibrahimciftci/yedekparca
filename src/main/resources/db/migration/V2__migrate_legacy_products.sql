INSERT INTO catalog_products(id,name,description,category,brand,oem_number,moderation_status,rejection_reason,reviewed_at,active,created_at,updated_at)
SELECT p.id,p.name,p.description,p.category,p.brand,p.oem_number,p.moderation_status,p.rejection_reason,p.reviewed_at,p.active,p.created_at,p.updated_at
FROM products p ON CONFLICT (id) DO NOTHING;
INSERT INTO product_offers(id,seller_id,catalog_product_id,legacy_product_id,price,stock_quantity,product_condition,moderation_status,rejection_reason,reviewed_at,active,price_updated_at,stock_updated_at,created_at,updated_at)
SELECT p.id,p.seller_id,p.id,p.id,p.price,p.stock_quantity,p.product_condition,p.moderation_status,p.rejection_reason,p.reviewed_at,p.active,COALESCE(p.updated_at,p.created_at),COALESCE(p.updated_at,p.created_at),p.created_at,p.updated_at
FROM products p ON CONFLICT (id) DO NOTHING;
UPDATE product_vehicle_compatibilities c SET catalog_product_id=c.product_id WHERE c.catalog_product_id IS NULL;
DO $$ BEGIN
 IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_compat_catalog_product') THEN
  ALTER TABLE product_vehicle_compatibilities ADD CONSTRAINT fk_compat_catalog_product FOREIGN KEY(catalog_product_id) REFERENCES catalog_products(id);
 END IF;
END $$;
SELECT setval(pg_get_serial_sequence('catalog_products','id'), GREATEST(COALESCE((SELECT max(id) FROM catalog_products),1),1), EXISTS(SELECT 1 FROM catalog_products));
SELECT setval(pg_get_serial_sequence('product_offers','id'), GREATEST(COALESCE((SELECT max(id) FROM product_offers),1),1), EXISTS(SELECT 1 FROM product_offers));
