package com.parca.yedekparca.config;

import com.parca.yedekparca.entity.*;
import com.parca.yedekparca.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Explicit, isolated demo fixture. Never active outside dev-demo. */
@Component
@Profile("dev-demo & !prod & !production")
@ConditionalOnProperty(name = "app.demo.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {
    private static final String PREFIX = "DEMO-PH-";
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final SellerRepository sellers;
    private final CatalogProductRepository catalogs;
    private final ProductOfferRepository offers;
    private final ProductVehicleCompatibilityRepository compatibilities;
    private final ProductCategoryRepository categories;
    private final ProductBrandRepository brands;
    private final CityRepository cities;
    private final DistrictRepository districts;
    private final VehicleModelRepository models;
    private final OfferPhotoRepository photos;
    private final PasswordEncoder encoder;
    private final String expectedDatabase;
    private final String seedPassword;

    public DemoDataSeeder(JdbcTemplate jdbc, UserRepository users, SellerRepository sellers,
            CatalogProductRepository catalogs, ProductOfferRepository offers,
            ProductVehicleCompatibilityRepository compatibilities, ProductCategoryRepository categories,
            ProductBrandRepository brands, CityRepository cities, DistrictRepository districts,
            VehicleModelRepository models, OfferPhotoRepository photos, PasswordEncoder encoder,
            @Value("${app.demo.expected-database:yedekparca_demo_db}") String expectedDatabase,
            @Value("${app.demo.seed-password:}") String seedPassword) {
        this.jdbc=jdbc; this.users=users; this.sellers=sellers; this.catalogs=catalogs; this.offers=offers;
        this.compatibilities=compatibilities; this.categories=categories; this.brands=brands; this.photos=photos;
        this.cities=cities; this.districts=districts; this.models=models; this.encoder=encoder;
        this.expectedDatabase=expectedDatabase; this.seedPassword=seedPassword;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if(args.containsOption("app.demo.cleanup")) return;
        String database=jdbc.queryForObject("select current_database()", String.class);
        if (!expectedDatabase.equals(database)) throw new IllegalStateException("Demo seeder refused database: expected " + expectedDatabase + ", connected to " + database);
        if (seedPassword == null || seedPassword.length() < 12) throw new IllegalStateException("Set DEMO_SEED_PASSWORD to a unique password of at least 12 characters.");
        Integer existing=jdbc.queryForObject("select count(*) from users where email like 'demo-ph-%@sandbox.invalid'",Integer.class);
        if (existing>0) { verifyCounts(); return; }
        List<ProductCategory> categoryList=categories.findAllByActiveTrueOrderByNameAsc();
        List<ProductBrand> brandList=brands.findAllByActiveTrueOrderByNameAsc();
        List<City> cityList=cities.findAll();
        List<VehicleModel> modelList=models.findAll().stream().filter(m->Boolean.TRUE.equals(m.getActive())).toList();
        if(categoryList.isEmpty()||brandList.isEmpty()||cityList.isEmpty()||modelList.isEmpty()) throw new IllegalStateException("Demo seeding requires migrated taxonomy, location and vehicle catalog data.");
        List<Seller> sellerList=new ArrayList<>();
        for(int i=0;i<50;i++) {
            City city=cityList.get(i%cityList.size()); List<District> districtList=districts.findAllByCityId(city.getId());
            if(districtList.isEmpty()) throw new IllegalStateException("City has no central districts: "+city.getName());
            District district=districtList.get(i%districtList.size());
            User user=users.save(User.builder().firstName("Demo").lastName(String.format("Satıcı %02d",i+1)).email(String.format("demo-ph-%03d@sandbox.invalid",i+1)).password(encoder.encode(seedPassword)).role(UserRole.SELLER).active(true).build());
            sellerList.add(sellers.save(Seller.builder().user(user).storeName(String.format("Demo Parça Atölyesi %02d",i+1)).phone(String.format("+90000000%04d",i+1)).whatsappPhone(String.format("+90000000%04d",i+1)).contactEmail(user.getEmail()).city(city.getName()).district(district.getName()).cityId(city.getId()).districtId(district.getId()).streetAddress("Demo adresi "+(i+1)).description("DEMO-PH test firması; gerçek sipariş kabul etmez.").workingHours("{\"monday\":\"09:00-18:00\",\"tuesday\":\"09:00-18:00\"}").status(SellerStatus.APPROVED).profileActive(true).reviewedAt(LocalDateTime.now()).build()));
        }
        List<CatalogProduct> catalogList=new ArrayList<>();
        String[] types={"Fren balatası","Yağ filtresi","Hava filtresi","Debriyaj seti","Amortisör","Far ünitesi","Su pompası","Ateşleme bobini","Radyatör","Salıncak"};
        for(int i=0;i<300;i++) {
            CatalogModerationStatus status=i<288?CatalogModerationStatus.APPROVED:(i<294?CatalogModerationStatus.PENDING:CatalogModerationStatus.REJECTED);
            CatalogProduct c=catalogs.save(CatalogProduct.builder().name(String.format("%s %s Seri %03d",types[i%types.length],brandList.get(i%brandList.size()).getName(),i+1)).description("DEMO-PH otomotiv yedek parça test ürünü. Referans numarası üretici OEM numarası değildir.").category(categoryList.get(i%categoryList.size()).getName()).brand(brandList.get(i%brandList.size()).getName()).oemNumber(String.format("DEMO-REF-%04d",i+1)).moderationStatus(status).active(true).reviewedAt(status==CatalogModerationStatus.APPROVED?LocalDateTime.now():null).build());
            catalogList.add(c);
            for(int n=0;n<7;n++){VehicleModel model=modelList.get((i*7+n)%modelList.size());compatibilities.save(ProductVehicleCompatibility.builder().catalogProduct(c).model(model).vehicleBrand(model.getBrand().getName()).vehicleModel(model.getName()).yearFrom(2005+(n%8)).yearTo(2015+(n%9)).engineInfo(n%2==0?"1.6 benzinli":"1.5 dizel").build());}
        }
        int[] distribution=DemoOfferDistribution.counts();
        int productIndex=0;
        for(int offerCount:distribution) {
            int i=productIndex;
            for(int n=0;n<offerCount;n++) {
            int sellerIndex=(i*7+n*13)%50; Seller seller=sellerList.get(sellerIndex); CatalogProduct catalog=catalogList.get(i);
            ProductModerationStatus status=i<288?(i%97==0?ProductModerationStatus.PENDING:ProductModerationStatus.APPROVED):(i<294?ProductModerationStatus.PENDING:ProductModerationStatus.REJECTED);
            LocalDateTime now=LocalDateTime.now();
            ProductOffer offer=offers.save(ProductOffer.builder().seller(seller).catalogProduct(catalog).offerDescription("DEMO-PH test ilanı · "+(n==0?"hızlı teslimat":"kontrollü kalite")).price(BigDecimal.valueOf(450L+(i*173L+n*89L)%24000L,2)).stockQuantity((i+n)%11==0?0:(i*3+n*7)%34+1).condition((n+i)%4==0?ProductCondition.USED:ProductCondition.NEW).moderationStatus(status).active(true).reviewedAt(status==ProductModerationStatus.APPROVED?now:null).priceUpdatedAt(now.minusDays((i+n)%46)).stockUpdatedAt(now.minusDays((i*2+n)%40)).build());
            int imageCount=switch((i+n)%10){case 0->1;case 1->3;case 2->5;default->0;};
            String[] imageFiles={"brake.svg","filter.svg","engine.svg"};
            for(int p=0;p<imageCount;p++){String filename=imageFiles[(i+p)%imageFiles.length];photos.save(OfferPhoto.builder().offer(offer).objectKey("demo-local/offer-"+offer.getId()+"-"+p+"-"+filename).publicUrl("/api/dev-demo/images/"+filename).displayOrder(p).primaryPhoto(p==0).build());}
            }
            productIndex++;
        }
        verifyCounts();
    }

    private void verifyCounts() {
        Integer sellerCount=jdbc.queryForObject("select count(*) from seller_applications where contact_email like 'demo-ph-%@sandbox.invalid'",Integer.class);
        Integer catalogCount=jdbc.queryForObject("select count(*) from catalog_products where oem_number like 'DEMO-REF-%'",Integer.class);
        Integer offerCount=jdbc.queryForObject("select count(*) from product_offers o join catalog_products c on c.id=o.catalog_product_id where c.oem_number like 'DEMO-REF-%'",Integer.class);
        Integer compatibilityCount=jdbc.queryForObject("select count(*) from product_vehicle_compatibilities v join catalog_products c on c.id=v.catalog_product_id where c.oem_number like 'DEMO-REF-%'",Integer.class);
        if(sellerCount!=50||catalogCount!=300||offerCount!=900||compatibilityCount!=2100) throw new IllegalStateException("Demo dataset counts do not match expected 50/300/900/2100; connected to demo DB, review data before rerunning.");
    }
}
