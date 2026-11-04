package com.parca.yedekparca;

import com.parca.yedekparca.dto.*;
import com.parca.yedekparca.entity.*;
import com.parca.yedekparca.repository.*;
import com.parca.yedekparca.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductWorkflowTests {

    @Autowired private ProductService productService;
    @Autowired private UserRepository userRepository;
    @Autowired private SellerRepository sellerRepository;

    @Test
    void approvedSellerCanCreatePendingActiveProduct() {
        Seller seller = createSeller(SellerStatus.APPROVED);

        ProductResponse response = productService.createSellerProduct(
                seller.getUser().getEmail(), request("Oil Filter", List.of()));

        assertEquals(ProductModerationStatus.PENDING, response.getModerationStatus());
        assertTrue(response.getActive());
        assertEquals(seller.getId(), response.getSellerId());
    }

    @Test
    void sellerWithoutApprovedApplicationCannotCreateProduct() {
        Seller seller = createSeller(SellerStatus.PENDING);

        assertThrows(IllegalArgumentException.class, () -> productService.createSellerProduct(
                seller.getUser().getEmail(), request("Oil Filter", List.of())));
    }

    @Test
    void sellerCannotReadOrUpdateAnotherSellersProduct() {
        Seller owner = createSeller(SellerStatus.APPROVED);
        Seller other = createSeller(SellerStatus.APPROVED);
        ProductResponse product = productService.createSellerProduct(
                owner.getUser().getEmail(), request("Brake Pad", List.of()));

        assertThrows(IllegalArgumentException.class, () -> productService.getSellerProduct(
                other.getUser().getEmail(), product.getId()));
        assertThrows(IllegalArgumentException.class, () -> productService.updateSellerProduct(
                other.getUser().getEmail(), product.getId(), updateRequest(product)));
    }

    @Test
    void contentEditsResetModerationButPriceAndStockEditsDoNot() {
        Seller seller = createSeller(SellerStatus.APPROVED);
        ProductResponse created = productService.createSellerProduct(
                seller.getUser().getEmail(), request("Air Filter", List.of()));
        ProductResponse approved = productService.approveProduct(created.getId());

        ProductUpdateRequest priceUpdate = updateRequest(approved);
        priceUpdate.setPrice(new BigDecimal("19.95"));
        priceUpdate.setStockQuantity(25);
        ProductResponse stillApproved = productService.updateSellerProduct(
                seller.getUser().getEmail(), created.getId(), priceUpdate);
        assertEquals(ProductModerationStatus.APPROVED, stillApproved.getModerationStatus());

        ProductUpdateRequest contentUpdate = updateRequest(stillApproved);
        contentUpdate.setName("Updated Air Filter");
        ProductResponse pendingAgain = productService.updateSellerProduct(
                seller.getUser().getEmail(), created.getId(), contentUpdate);
        assertEquals(ProductModerationStatus.PENDING, pendingAgain.getModerationStatus());
        assertNull(pendingAgain.getReviewedAt());
    }

    @Test
    void adminCanApproveOrRejectPendingProductsAndRejectionNeedsReason() {
        Seller seller = createSeller(SellerStatus.APPROVED);
        ProductResponse toApprove = productService.createSellerProduct(
                seller.getUser().getEmail(), request("Approved Product", List.of()));
        ProductResponse approved = productService.approveProduct(toApprove.getId());
        assertEquals(ProductModerationStatus.APPROVED, approved.getModerationStatus());
        assertNotNull(approved.getReviewedAt());
        assertThrows(IllegalArgumentException.class, () -> productService.approveProduct(toApprove.getId()));

        ProductResponse toReject = productService.createSellerProduct(
                seller.getUser().getEmail(), request("Rejected Product", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> productService.rejectProduct(toReject.getId(), "  "));
        ProductResponse rejected = productService.rejectProduct(toReject.getId(), "Eksik ürün bilgisi");
        assertEquals(ProductModerationStatus.REJECTED, rejected.getModerationStatus());
        assertEquals("Eksik ürün bilgisi", rejected.getRejectionReason());
        assertNotNull(rejected.getReviewedAt());
    }

    @Test
    void publicCatalogHidesPendingAndInactiveProducts() {
        Seller seller = createSeller(SellerStatus.APPROVED);
        ProductResponse pending = productService.createSellerProduct(
                seller.getUser().getEmail(), request("Private Product", List.of()));
        assertThrows(IllegalArgumentException.class, () -> productService.getPublicProduct(pending.getId()));

        productService.approveProduct(pending.getId());
        productService.updateActiveStatus(pending.getId(), false);
        assertThrows(IllegalArgumentException.class, () -> productService.getPublicProduct(pending.getId()));

        ProductSearchCriteria criteria = emptyCatalogCriteria();
        ProductPageResponse<PublicProductResponse> products =
                productService.getPublicProducts(criteria, 0, 100, "createdAt", "desc");
        assertFalse(products.getContent().stream().anyMatch(item -> item.getId().equals(pending.getId())));
    }

    @Test
    void vehicleCompatibilityExistsFilterDoesNotDuplicateProductsAndSearchIsPaged() {
        Seller seller = createSeller(SellerStatus.APPROVED);
        ProductVehicleCompatibilityRequest fitmentOne = compatibility("Acme", "Roadster", 2018, 2022, "1.6");
        ProductVehicleCompatibilityRequest fitmentTwo = compatibility("ACME", "ROADSTER", 2018, 2022, "2.0");
        ProductResponse matching = productService.createSellerProduct(seller.getUser().getEmail(),
                request("Unique Wheel Bearing", List.of(fitmentOne, fitmentTwo)));
        productService.approveProduct(matching.getId());

        ProductResponse second = productService.createSellerProduct(seller.getUser().getEmail(),
                request("Unique Wheel Hub", List.of()));
        productService.approveProduct(second.getId());

        ProductSearchCriteria vehicleCriteria = new ProductSearchCriteria(
                null, null, null, null, null, null, null,
                "acme", "Roadster", 2020, null, null, null, null);
        ProductPageResponse<PublicProductResponse> vehicleResults = productService.getPublicProducts(
                vehicleCriteria, 0, 1, "createdAt", "desc");
        assertEquals(1, vehicleResults.getTotalElements());
        assertEquals(matching.getId(), vehicleResults.getContent().get(0).getId());

        ProductSearchCriteria searchCriteria = new ProductSearchCriteria(
                "Unique Wheel", null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        ProductPageResponse<PublicProductResponse> page = productService.getPublicProducts(
                searchCriteria, 0, 1, "createdAt", "desc");
        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void duplicateVehicleCompatibilityIsRejected() {
        Seller seller = createSeller(SellerStatus.APPROVED);
        ProductVehicleCompatibilityRequest first = compatibility("Acme", "Roadster", 2020, 2022, null);
        ProductVehicleCompatibilityRequest duplicate = compatibility("ACME", "ROADSTER", 2020, 2022, " ");

        assertThrows(IllegalArgumentException.class, () -> productService.createSellerProduct(
                seller.getUser().getEmail(), request("Duplicate Fitment", List.of(first, duplicate))));
    }

    private Seller createSeller(SellerStatus status) {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        User user = userRepository.save(User.builder()
                .firstName("ProductSeller")
                .lastName(suffix)
                .email("product-seller-" + suffix + "@example.test")
                .password("test-password")
                .role(UserRole.SELLER)
                .active(true)
                .build());
        return sellerRepository.save(Seller.builder()
                .user(user)
                .storeName("Store " + suffix)
                .phone("5551234567")
                .city("Istanbul")
                .district("Kadikoy")
                .status(status)
                .build());
    }

    private ProductCreateRequest request(String name, List<ProductVehicleCompatibilityRequest> compatibilities) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName(name);
        request.setDescription("Product description");
        request.setCategory("Engine");
        request.setBrand("PartsCo");
        request.setOemNumber("OEM-" + UUID.randomUUID());
        request.setPrice(new BigDecimal("10.50"));
        request.setStockQuantity(5);
        request.setCondition(ProductCondition.NEW);
        request.setVehicleCompatibilities(compatibilities);
        return request;
    }

    private ProductUpdateRequest updateRequest(ProductResponse product) {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName(product.getName());
        request.setDescription(product.getDescription());
        request.setCategory(product.getCategory());
        request.setBrand(product.getBrand());
        request.setOemNumber(product.getOemNumber());
        request.setPrice(product.getPrice());
        request.setStockQuantity(product.getStockQuantity());
        request.setCondition(product.getCondition());
        request.setImageUrl(product.getImageUrl());
        request.setVehicleCompatibilities(product.getVehicleCompatibilities().stream().map(item -> {
            ProductVehicleCompatibilityRequest requestItem = new ProductVehicleCompatibilityRequest();
            requestItem.setVehicleBrand(item.getVehicleBrand());
            requestItem.setVehicleModel(item.getVehicleModel());
            requestItem.setYearFrom(item.getYearFrom());
            requestItem.setYearTo(item.getYearTo());
            requestItem.setEngineInfo(item.getEngineInfo());
            return requestItem;
        }).toList());
        return request;
    }

    private ProductVehicleCompatibilityRequest compatibility(
            String brand, String model, int yearFrom, int yearTo, String engine) {
        ProductVehicleCompatibilityRequest request = new ProductVehicleCompatibilityRequest();
        request.setVehicleBrand(brand);
        request.setVehicleModel(model);
        request.setYearFrom(yearFrom);
        request.setYearTo(yearTo);
        request.setEngineInfo(engine);
        return request;
    }

    private ProductSearchCriteria emptyCatalogCriteria() {
        return new ProductSearchCriteria(null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }
}
