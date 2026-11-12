package com.parca.yedekparca.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.Optional;

@Component
@Profile("dev-demo & !prod & !production")
public class DemoCleanupRunner implements ApplicationRunner {
    private final JdbcTemplate jdbc; private final TransactionTemplate tx; private final String expected;
    public DemoCleanupRunner(JdbcTemplate jdbc, TransactionTemplate tx, @Value("${app.demo.expected-database:yedekparca_demo_db}") String expected){this.jdbc=jdbc;this.tx=tx;this.expected=expected;}
    @Override public void run(ApplicationArguments args){
        if(!args.containsOption("app.demo.cleanup"))return;
        String db=jdbc.queryForObject("select current_database()",String.class);
        if(!expected.equals(db))throw new IllegalStateException("Cleanup refused database "+db+"; expected "+expected);
        Integer count=jdbc.queryForObject("select count(*) from users where email like 'demo-ph-%@sandbox.invalid'",Integer.class);
        boolean confirm=Optional.ofNullable(args.getOptionValues("app.demo.cleanup")).orElse(List.of()).stream().anyMatch("CONFIRM"::equals);
        System.out.println("Demo cleanup preview: "+count+" marked users; database="+db+"; mode="+(confirm?"DELETE":"DRY-RUN"));
        if(!confirm)return;
        tx.executeWithoutResult(status->{
            jdbc.update("delete from complaints where reporter_id in (select id from users where email like 'demo-ph-%@sandbox.invalid') or offer_id in (select o.id from product_offers o join catalog_products c on c.id=o.catalog_product_id where c.oem_number like 'DEMO-REF-%')");
            jdbc.update("delete from user_favorites where user_id in (select id from users where email like 'demo-ph-%@sandbox.invalid') or catalog_product_id in (select id from catalog_products where oem_number like 'DEMO-REF-%')");
            jdbc.update("delete from offer_photos where offer_id in (select o.id from product_offers o join catalog_products c on c.id=o.catalog_product_id where c.oem_number like 'DEMO-REF-%')");
            jdbc.update("delete from product_offers where catalog_product_id in (select id from catalog_products where oem_number like 'DEMO-REF-%')");
            jdbc.update("delete from product_vehicle_compatibilities where catalog_product_id in (select id from catalog_products where oem_number like 'DEMO-REF-%')");
            jdbc.update("delete from catalog_products where oem_number like 'DEMO-REF-%'");
            jdbc.update("delete from seller_applications where contact_email like 'demo-ph-%@sandbox.invalid'");
            jdbc.update("delete from users where email like 'demo-ph-%@sandbox.invalid'");
        });
    }
}
