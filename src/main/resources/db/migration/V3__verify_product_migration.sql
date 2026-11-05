DO $$
DECLARE legacy_count BIGINT; catalog_count BIGINT; offer_count BIGINT; compat_count BIGINT; migrated_compat_count BIGINT;
BEGIN
 SELECT count(*) INTO legacy_count FROM products;
 SELECT count(*) INTO catalog_count FROM catalog_products c JOIN products p ON p.id=c.id;
 SELECT count(*) INTO offer_count FROM product_offers o JOIN products p ON p.id=o.legacy_product_id;
 SELECT count(*) INTO compat_count FROM product_vehicle_compatibilities;
 SELECT count(*) INTO migrated_compat_count FROM product_vehicle_compatibilities WHERE catalog_product_id IS NOT NULL;
 IF legacy_count<>catalog_count OR legacy_count<>offer_count THEN RAISE EXCEPTION 'Legacy product migration count mismatch: products %, catalog %, offers %',legacy_count,catalog_count,offer_count; END IF;
 IF compat_count<>migrated_compat_count THEN RAISE EXCEPTION 'Vehicle compatibility migration count mismatch: legacy %, migrated %',compat_count,migrated_compat_count; END IF;
END $$;
