package com.parca.yedekparca;

import com.parca.yedekparca.dto.*;
import com.parca.yedekparca.entity.*;
import com.parca.yedekparca.repository.*;
import com.parca.yedekparca.service.MarketplaceService;
import com.parca.yedekparca.service.ObjectStorageService;
import com.parca.yedekparca.service.VehicleCatalogService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MarketplaceWorkflowTests {
    @Autowired
    MarketplaceService service;
    @Autowired
    UserRepository users;
    @Autowired
    SellerRepository sellers;
    @Autowired
    CatalogProductRepository catalogs;
    @Autowired
    ProductOfferRepository offers;
    @Autowired
    OfferPhotoRepository photos;
    @MockitoBean
    ObjectStorageService storage;
    @Autowired
    VehicleCatalogService vehicleCatalog;

    @Test
    void approvedSellerCanProposeCatalogAndOfferStartsPending() {
        Seller seller = seller();
        long before = offers.count();
        OfferView offer = service.createOffer(seller.getUser().getEmail(), request(null));
        assertEquals(ProductModerationStatus.PENDING, offer.moderationStatus());
        assertNotNull(catalogs.findById(offer.catalogProductId()).orElseThrow());
        assertEquals(before + 1, offers.count());
        assertTrue(service.adminOffers(ProductModerationStatus.PENDING, 0, 100).stream()
                .anyMatch(item -> item.id().equals(offer.id())));
        assertEquals(offer.id(), service.adminOffer(offer.id()).id());
        assertFalse(service.publicCatalog("Part", null, null, null, null, null, null,
                null, null, null, null, 0, 100).getContent().stream()
                .anyMatch(item -> item.id().equals(offer.catalogProductId())));

        service.reviewCatalog(offer.catalogProductId(), true, null);
        service.reviewOffer(offer.id(), true, null);
        CatalogView visible = service.publicCatalog("Part", null, null, null, null, null, null,
                null, null, null, null, 0, 100).getContent().stream()
                .filter(item -> item.id().equals(offer.catalogProductId())).findFirst().orElseThrow();
        assertEquals(1, visible.offers().size());
        assertEquals(offer.id(), visible.offers().get(0).id());
        assertEquals(new BigDecimal("15.00"), visible.offers().get(0).price());
    }

    @Test
    void offerApprovalDoesNotApproveItsPendingCatalogOrExposeItToCustomers() {
        Seller seller = seller();
        OfferView offer = service.createOffer(seller.getUser().getEmail(), request(null));

        service.reviewOffer(offer.id(), true, null);

        assertEquals(CatalogModerationStatus.PENDING,
                catalogs.findById(offer.catalogProductId()).orElseThrow().getModerationStatus());
        assertFalse(service.publicCatalog(null, null, null, null, null, null, null,
                null, null, null, null, 0, 100).getContent().stream()
                .anyMatch(item -> item.id().equals(offer.catalogProductId())));

        service.reviewCatalog(offer.catalogProductId(), true, null);
        CatalogView visible = service.publicCatalog(null, null, null, null, null, null, null,
                null, null, null, null, 0, 100).getContent().stream()
                .filter(item -> item.id().equals(offer.catalogProductId())).findFirst().orElseThrow();
        assertEquals(offer.id(), visible.offers().get(0).id());
    }

    @Test
    void adminOfferListSupportsMarketplaceFiltersAndWorkerPhotoUrls() {
        Seller seller = seller();
        String suffix = UUID.randomUUID().toString();
        CatalogProduct catalog = catalogs.save(CatalogProduct.builder().name("Admin Filter " + suffix)
                .category("Admin Category " + suffix).brand("Admin Brand " + suffix)
                .moderationStatus(CatalogModerationStatus.APPROVED).active(true).build());
        OfferView created = service.createOffer(seller.getUser().getEmail(), request(catalog.getId()));
        service.reviewOffer(created.id(), true, null);

        ProductOffer entity = offers.findById(created.id()).orElseThrow();
        String key = "offers/" + UUID.randomUUID() + ".jpg";
        String storedUrl = "https://old-bucket.r2.dev/" + key;
        String workerUrl = "https://yedekparca-image-proxy.yedekparca.workers.dev/" + key;
        OfferPhoto photo = OfferPhoto.builder().offer(entity).objectKey(key).publicUrl(storedUrl)
                .displayOrder(0).primaryPhoto(true).build();
        entity.getPhotos().add(photo);
        photos.save(photo);
        when(storage.publicUrl(key, storedUrl)).thenReturn(workerUrl);

        var filtered = service.adminOffers("Admin Filter " + suffix, "Admin Category " + suffix,
                "Admin Brand " + suffix, seller.getId(), ProductModerationStatus.APPROVED, true, 0, 1);

        assertEquals(1, filtered.getTotalElements());
        assertEquals(1, filtered.getContent().size());
        assertEquals(workerUrl, filtered.getContent().get(0).photos().get(0).url());

        service.setOfferActive(created.id(), false);
        assertEquals(0, service.adminOffers(null, null, null, seller.getId(),
                ProductModerationStatus.APPROVED, true, 0, 10).getTotalElements());
        assertEquals(1, service.adminOffers(null, null, null, seller.getId(),
                ProductModerationStatus.APPROVED, false, 0, 10).getTotalElements());
    }

    @Test
    void uploadedImageObjectKeyIsStoredOnTheOwningOffer() throws Exception {
        Seller seller = seller();
        CatalogProduct catalog = catalogs.save(CatalogProduct.builder().name("Image Part").category("Engine")
                .brand("PartsCo").moderationStatus(CatalogModerationStatus.APPROVED).active(true).build());
        OfferView offer = service.createOffer(seller.getUser().getEmail(), request(catalog.getId()));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);
        MockMultipartFile file = new MockMultipartFile("file", "part.png", "image/png", bytes.toByteArray());
        String storedUrl = "https://images.example.test/offers/test-image.png";
        when(storage.newKey("png")).thenReturn("offers/test-image.png");
        when(storage.put("offers/test-image.png", "image/png", file.getBytes())).thenReturn(storedUrl);
        when(storage.publicUrl("offers/test-image.png", storedUrl)).thenReturn(storedUrl);

        String url = service.addPhoto(seller.getUser().getEmail(), offer.id(), file);

        assertEquals(storedUrl, url);
        OfferView updated = service.myOffer(seller.getUser().getEmail(), offer.id());
        assertEquals("offers/test-image.png", updated.photos().get(0).url().substring("https://images.example.test/".length()));
        assertEquals(url, updated.photos().get(0).url());
    }

    @Test
    void marketplaceResponseResolvesLegacyPhotoUrlFromObjectKeyAndCurrentBase() {
        Seller seller = seller();
        CatalogProduct catalog = catalogs.save(CatalogProduct.builder().name("Existing Image Part").category("Engine")
                .brand("PartsCo").moderationStatus(CatalogModerationStatus.APPROVED).active(true).build());
        OfferView offer = service.createOffer(seller.getUser().getEmail(), request(catalog.getId()));
        ProductOffer entity = offers.findById(offer.id()).orElseThrow();
        String key = "offers/" + UUID.randomUUID() + ".jpg";
        String storedUrl = "https://old-bucket.r2.dev/" + key;
        String workerUrl = "https://yedekparca-image-proxy.yedekparca.workers.dev/" + key;
        OfferPhoto photo = OfferPhoto.builder().offer(entity).objectKey(key).publicUrl(storedUrl)
                .displayOrder(0).primaryPhoto(true).build();
        entity.getPhotos().add(photo);
        photos.save(photo);
        when(storage.publicUrl(key, storedUrl)).thenReturn(workerUrl);

        OfferView response = service.myOffer(seller.getUser().getEmail(), offer.id());

        assertEquals(workerUrl, response.photos().get(0).url());
    }

    @Test
    void adminCanInspectAndRejectPendingSellerOffer() {
        Seller seller = seller();
        OfferView offer = service.createOffer(seller.getUser().getEmail(), request(null));

        OfferView detail = service.adminOffer(offer.id());
        assertEquals(offer.catalogProductId(), detail.catalogProductId());
        assertEquals(ProductModerationStatus.PENDING, detail.moderationStatus());

        OfferView rejected = service.reviewOffer(offer.id(), false, "Eksik ilan bilgisi");
        assertEquals(ProductModerationStatus.REJECTED, rejected.moderationStatus());
        assertEquals("Eksik ilan bilgisi", rejected.rejectionReason());
        assertFalse(service.publicCatalog("Part", null, null, null, null, null, null,
                null, null, null, null, 0, 100).getContent().stream()
                .anyMatch(item -> item.id().equals(offer.catalogProductId())));
    }

    @Test
    void offersRequireApprovedCatalogAndShowOnlyApprovedOffers() {
        Seller seller = seller();
        CatalogProduct catalog = catalogs.save(CatalogProduct.builder().name("Engine Part").category("Engine").brand("PartsCo").moderationStatus(CatalogModerationStatus.APPROVED).active(true).build());
        OfferView pending = service.createOffer(seller.getUser().getEmail(), request(catalog.getId()));
        assertTrue(service.catalogDetail(catalog.getId()).offers().isEmpty());
        service.reviewOffer(pending.id(), true, null);
        assertEquals(1, service.catalogDetail(catalog.getId()).offers().size());
        service.setOfferActive(pending.id(), false);
        assertTrue(service.catalogDetail(catalog.getId()).offers().isEmpty());
    }

    @Test
    void compatibleCatalogFiltersDeduplicateAndSortPublicOffers() {
        Seller firstSeller = seller();
        Seller secondSeller = seller();
        CatalogProduct catalog = catalogs.save(CatalogProduct.builder()
                .name("Wheel Bearing").category("Chassis").brand("PartsCo")
                .moderationStatus(CatalogModerationStatus.APPROVED).active(true).build());
        catalog.getVehicleCompatibilities().add(ProductVehicleCompatibility.builder()
                .catalogProduct(catalog).vehicleBrand("Acme").vehicleModel("Touring")
                .yearFrom(2020).yearTo(2024).engineInfo("2.0").build());
        catalog = catalogs.save(catalog);
        Long catalogId = catalog.getId();

        ProductVehicleCompatibilityRequest legacyFitment = new ProductVehicleCompatibilityRequest();
        legacyFitment.setVehicleBrand("Acme");
        legacyFitment.setVehicleModel("Touring");
        legacyFitment.setYearFrom(2020);
        legacyFitment.setYearTo(2024);
        legacyFitment.setEngineInfo("2.0");
        OfferWriteRequest firstRequest = new OfferWriteRequest(catalogId, "Wheel Bearing", null,
                "Chassis", "PartsCo", null, new BigDecimal("15.00"), 2, ProductCondition.NEW,
                java.util.List.of(legacyFitment));
        OfferView first = service.createOffer(firstSeller.getUser().getEmail(), firstRequest);
        service.reviewOffer(first.id(), true, null);
        OfferWriteRequest cheaperRequest = new OfferWriteRequest(catalogId, "Wheel Bearing", null,
                "Chassis", "PartsCo", null, new BigDecimal("9.00"), 1, ProductCondition.NEW,
                java.util.List.of(legacyFitment));
        OfferView cheaper = service.createOffer(secondSeller.getUser().getEmail(), cheaperRequest);
        service.reviewOffer(cheaper.id(), true, null);

        org.springframework.data.domain.Page<CatalogView> result = service.publicCatalog("Wheel Bearing", null, null, null,
                null, null, ProductCondition.NEW, "Acme", "Touring", 2022, "2.0", 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(2, result.getContent().get(0).offers().size());
        assertEquals(cheaper.id(), result.getContent().get(0).offers().get(0).id());
        assertThrows(IllegalArgumentException.class,
                () -> service.createOffer(firstSeller.getUser().getEmail(), firstRequest));
    }

    @Test
    void sellerCompatibilityUsesCentralModelAndPublicCatalogFiltersByItsIds() {
        Seller seller = seller();
        var type = vehicleCatalog.listTypes().get(0);
        var brand = vehicleCatalog.listBrands(type.id()).get(0);
        var model = vehicleCatalog.listModels(brand.id(), type.id()).get(0);
        ProductVehicleCompatibilityRequest fitment = new ProductVehicleCompatibilityRequest();
        fitment.setVehicleModelId(model.id());
        fitment.setYearFrom(2010);
        fitment.setYearTo(2020);
        fitment.setEngineInfo("2.0");

        OfferView offer = service.createOffer(seller.getUser().getEmail(), new OfferWriteRequest(null,
                "Central vehicle part", "description", "Engine", "PartsCo", null,
                new BigDecimal("25.00"), 2, ProductCondition.NEW, java.util.List.of(fitment)));
        service.reviewCatalog(offer.catalogProductId(), true, null);
        service.reviewOffer(offer.id(), true, null);

        CatalogProduct stored = catalogs.findById(offer.catalogProductId()).orElseThrow();
        assertEquals(model.id(), stored.getVehicleCompatibilities().get(0).getModel().getId());
        assertEquals(model.id(), service.myOffer(seller.getUser().getEmail(), offer.id())
                .vehicleCompatibilities().get(0).getVehicleModelId());
        var filtered = service.publicCatalog(null, null, null, null, null, null, null,
                null, null, null, null, type.id(), brand.id(), model.id(), 0, 20);
        assertTrue(filtered.getContent().stream().anyMatch(item -> item.id().equals(offer.catalogProductId())));

        ProductVehicleCompatibilityRequest duplicate = new ProductVehicleCompatibilityRequest();
        duplicate.setVehicleModelId(model.id());
        duplicate.setYearFrom(2021);
        duplicate.setYearTo(2024);
        assertThrows(IllegalArgumentException.class, () -> service.createOffer(seller.getUser().getEmail(),
                new OfferWriteRequest(null, "Duplicate vehicle part", null, "Engine", "PartsCo", null,
                        new BigDecimal("25.00"), 2, ProductCondition.NEW, java.util.List.of(fitment, duplicate))));
    }

    @Test
    void sellerCannotAccessAnotherSellersOfferAndPricesMustBePositive() {
        Seller owner = seller(), other = seller();
        CatalogProduct c = catalogs.save(CatalogProduct.builder().name("Filter").category("Engine").brand("Brand").moderationStatus(CatalogModerationStatus.APPROVED).active(true).build());
        OfferView offer = service.createOffer(owner.getUser().getEmail(), request(c.getId()));
        assertThrows(IllegalArgumentException.class, () -> service.myOffer(other.getUser().getEmail(), offer.id()));
    }

    @Test
    void complaintsRequireCustomerAndApprovedSellerContext() {
        Seller seller = seller();
        User customer = user(UserRole.CUSTOMER);
        ComplaintView view = service.createComplaint(customer.getEmail(), new ComplaintCreateRequest(seller.getId(), null, ComplaintCategory.OTHER, "Fiyat bildirimi"));
        assertEquals(ComplaintStatus.OPEN, view.status());
        assertThrows(IllegalArgumentException.class, () -> service.myFavorites(seller.getUser().getEmail()));
    }

    private Seller seller() {
        User u = user(UserRole.SELLER);
        return sellers.save(Seller.builder().user(u).storeName("Store").phone("5551234567").city("Istanbul").district("Kadikoy").status(SellerStatus.APPROVED).build());
    }

    private User user(UserRole role) {
        String id = UUID.randomUUID().toString().replace("-", "");
        return users.save(User.builder().firstName("Test").lastName("User").email(id + "@example.test").password("encoded").role(role).active(true).build());
    }

    private OfferWriteRequest request(Long catalogId) {
        return new OfferWriteRequest(catalogId, "Part", "desc", "Engine", "PartsCo", null, new BigDecimal("15.00"), 2, ProductCondition.NEW, java.util.List.of());
    }
}
