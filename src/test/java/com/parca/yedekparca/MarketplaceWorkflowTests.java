package com.parca.yedekparca;

import com.parca.yedekparca.dto.*;
import com.parca.yedekparca.entity.*;
import com.parca.yedekparca.repository.*;
import com.parca.yedekparca.service.MarketplaceService;
import com.parca.yedekparca.service.ObjectStorageService;
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
    @MockitoBean
    ObjectStorageService storage;

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
    void uploadedImageObjectKeyIsStoredOnTheOwningOffer() throws Exception {
        Seller seller = seller();
        CatalogProduct catalog = catalogs.save(CatalogProduct.builder().name("Image Part").category("Engine")
                .brand("PartsCo").moderationStatus(CatalogModerationStatus.APPROVED).active(true).build());
        OfferView offer = service.createOffer(seller.getUser().getEmail(), request(catalog.getId()));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);
        MockMultipartFile file = new MockMultipartFile("file", "part.png", "image/png", bytes.toByteArray());
        when(storage.newKey("png")).thenReturn("offers/test-image.png");
        when(storage.put("offers/test-image.png", "image/png", file.getBytes())).thenReturn("https://images.example.test/offers/test-image.png");

        String url = service.addPhoto(seller.getUser().getEmail(), offer.id(), file);

        assertEquals("https://images.example.test/offers/test-image.png", url);
        OfferView updated = service.myOffer(seller.getUser().getEmail(), offer.id());
        assertEquals("offers/test-image.png", updated.photos().get(0).url().substring("https://images.example.test/".length()));
        assertEquals(url, updated.photos().get(0).url());
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

        OfferView first = service.createOffer(firstSeller.getUser().getEmail(), request(catalogId));
        service.reviewOffer(first.id(), true, null);
        OfferWriteRequest cheaperRequest = new OfferWriteRequest(catalogId, "Wheel Bearing", null,
                "Chassis", "PartsCo", null, new BigDecimal("9.00"), 1, ProductCondition.NEW, java.util.List.of());
        OfferView cheaper = service.createOffer(secondSeller.getUser().getEmail(), cheaperRequest);
        service.reviewOffer(cheaper.id(), true, null);

        org.springframework.data.domain.Page<CatalogView> result = service.publicCatalog("Wheel Bearing", null, null, null,
                null, null, ProductCondition.NEW, "Acme", "Touring", 2022, "2.0", 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(2, result.getContent().get(0).offers().size());
        assertEquals(cheaper.id(), result.getContent().get(0).offers().get(0).id());
        assertThrows(IllegalArgumentException.class,
                () -> service.createOffer(firstSeller.getUser().getEmail(), request(catalogId)));
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
