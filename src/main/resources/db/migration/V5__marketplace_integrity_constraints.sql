CREATE UNIQUE INDEX uk_offer_seller_catalog_condition ON product_offers(seller_id,catalog_product_id,product_condition);
ALTER TABLE product_vehicle_compatibilities ADD CONSTRAINT chk_compat_year_range CHECK(year_from<=year_to) NOT VALID;
ALTER TABLE product_offers ADD CONSTRAINT chk_offer_positive_price CHECK(price>0) NOT VALID;
