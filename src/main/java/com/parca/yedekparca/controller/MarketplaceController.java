package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.*;
import com.parca.yedekparca.entity.*;
import com.parca.yedekparca.service.MarketplaceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Validated
@RestController
public class MarketplaceController {
    private final MarketplaceService service;

    public MarketplaceController(MarketplaceService service) {
        this.service = service;
    }

    @GetMapping("/api/catalog/products")
    public ApiResponse<Page<CatalogView>> catalog(@RequestParam(required = false) String search,
            @RequestParam(required = false) String category, @RequestParam(required = false) String brand,
            @RequestParam(required = false) String oemNumber,
            @RequestParam(required = false) @jakarta.validation.constraints.DecimalMin("0.00") java.math.BigDecimal minPrice,
            @RequestParam(required = false) @jakarta.validation.constraints.DecimalMin("0.00") java.math.BigDecimal maxPrice,
            @RequestParam(required = false) ProductCondition condition,
            @RequestParam(required = false) String vehicleBrand, @RequestParam(required = false) String vehicleModel,
            @RequestParam(required = false) Long vehicleTypeId, @RequestParam(required = false) Long vehicleBrandId,
            @RequestParam(required = false) Long vehicleModelId,
            @RequestParam(required = false) @Min(1886) Integer modelYear,
            @RequestParam(required = false) String engineInfo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Katalog ürünleri.", service.publicCatalog(search, category, brand, oemNumber,
                minPrice, maxPrice, condition, vehicleBrand, vehicleModel, modelYear, engineInfo,
                vehicleTypeId, vehicleBrandId, vehicleModelId, page, size));
    }

    @GetMapping("/api/catalog/products/{id}")
    public ApiResponse<CatalogView> detail(@PathVariable Long id) {
        return ApiResponse.success("Ürün ve firma teklifleri.", service.catalogDetail(id));
    }

    @GetMapping("/api/sellers/{id}/profile")
    public ApiResponse<SellerProfileView> publicProfile(@PathVariable Long id) {
        return ApiResponse.success("Firma profili.", service.publicProfile(id));
    }

    @GetMapping("/api/seller/profile")
    public ApiResponse<SellerProfileView> myProfile(Authentication a) {
        return ApiResponse.success("Firma profili.", service.myProfile(a.getName()));
    }

    @GetMapping("/api/seller/catalog")
    public ApiResponse<List<CatalogView>> sellerCatalog(@RequestParam(required = false) String search) {
        return ApiResponse.success("Onaylı katalog ürünleri.", service.sellerCatalogSearch(search));
    }

    @PutMapping("/api/seller/profile")
    public ApiResponse<SellerProfileView> updateProfile(Authentication a, @Valid @RequestBody SellerProfileUpdateRequest r) {
        return ApiResponse.success("Firma profili güncellendi.", service.updateProfile(a.getName(), r));
    }

    @PostMapping("/api/seller/offers")
    public ResponseEntity<ApiResponse<OfferView>> create(Authentication a, @Valid @RequestBody OfferWriteRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("İlan incelemeye gönderildi.", service.createOffer(a.getName(), r)));
    }

    @GetMapping("/api/seller/offers")
    public ApiResponse<Page<OfferView>> mine(Authentication a, @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("İlanlar.", service.myOffers(a.getName(), page, size));
    }

    @GetMapping("/api/seller/offers/{id}")
    public ApiResponse<OfferView> mineOne(Authentication a, @PathVariable Long id) {
        return ApiResponse.success("İlan detayı.", service.myOffer(a.getName(), id));
    }

    @PutMapping("/api/seller/offers/{id}")
    public ApiResponse<OfferView> update(Authentication a, @PathVariable Long id, @Valid @RequestBody OfferWriteRequest r) {
        return ApiResponse.success("İlan güncellendi.", service.updateOffer(a.getName(), id, r));
    }

    @PostMapping(value = "/api/seller/offers/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> photo(Authentication a, @PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Görsel yüklendi.", service.addPhoto(a.getName(), id, file)));
    }

    @PutMapping("/api/seller/offers/{id}/photos/order")
    public ApiResponse<Void> reorderPhotos(Authentication a, @PathVariable Long id, @Valid @RequestBody PhotoOrderRequest r) {
        service.reorderPhotos(a.getName(), id, r.photoIds());
        return ApiResponse.success("Fotoğraf sırası güncellendi.", null);
    }

    @DeleteMapping("/api/seller/photos/{id}")
    public ApiResponse<Void> deletePhoto(Authentication a, @PathVariable Long id) {
        service.deletePhoto(a.getName(), id);
        return ApiResponse.success("Görsel silindi.", null);
    }

    @GetMapping("/api/admin/offers")
    public ApiResponse<Page<OfferView>> adminOffers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) ProductModerationStatus status,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("İlanlar.", service.adminOffers(search, category, brand, sellerId, status, active, page, size));
    }

    @GetMapping("/api/admin/offers/{id}")
    public ApiResponse<OfferView> adminOffer(@PathVariable Long id) {
        return ApiResponse.success("Offer detail.", service.adminOffer(id));
    }

    @GetMapping("/api/admin/catalog")
    public ApiResponse<Page<CatalogView>> adminCatalog(@RequestParam(required = false) CatalogModerationStatus status, @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Katalog ürünleri.", service.adminCatalogs(status, page, size));
    }

    @GetMapping("/api/admin/catalog/{id}")
    public ApiResponse<CatalogView> adminCatalogDetail(@PathVariable Long id) {
        return ApiResponse.success("Catalog product detail.", service.adminCatalog(id));
    }

    @GetMapping("/api/admin/sellers/{id}/profile")
    public ApiResponse<SellerProfileView> adminSellerProfile(@PathVariable Long id) {
        return ApiResponse.success("Firma profili.", service.adminSellerProfile(id));
    }

    @PutMapping("/api/admin/sellers/{id}/profile/active")
    public ApiResponse<SellerProfileView> adminSellerProfileActive(@PathVariable Long id, @Valid @RequestBody ProductActiveRequest request) {
        return ApiResponse.success("Seller profile visibility updated.", service.setSellerProfileActive(id, request.getActive()));
    }

    @PutMapping("/api/admin/offers/{id}/approve")
    public ApiResponse<OfferView> approveOffer(@PathVariable Long id) {
        return ApiResponse.success("İlan onaylandı.", service.reviewOffer(id, true, null));
    }

    @PutMapping("/api/admin/offers/{id}/active")
    public ApiResponse<OfferView> activeOffer(@PathVariable Long id, @Valid @RequestBody ProductActiveRequest r) {
        return ApiResponse.success("İlan aktifliği güncellendi.", service.setOfferActive(id, r.getActive()));
    }

    @PutMapping("/api/admin/offers/{id}/reject")
    public ApiResponse<OfferView> rejectOffer(@PathVariable Long id, @Valid @RequestBody ModerationRequest r) {
        return ApiResponse.success("İlan reddedildi.", service.reviewOffer(id, false, r.reason()));
    }

    @PutMapping("/api/admin/catalog/{id}/approve")
    public ApiResponse<CatalogView> approveCatalog(@PathVariable Long id) {
        CatalogProduct c = service.reviewCatalog(id, true, null);
        return ApiResponse.success("Katalog ürünü onaylandı.", new CatalogView(c.getId(), c.getName(), c.getDescription(), c.getCategory(), c.getBrand(), c.getOemNumber(), c.getModerationStatus().name(), c.getActive(), List.of(), List.of()));
    }

    @PutMapping("/api/admin/catalog/{id}/reject")
    public ApiResponse<CatalogView> rejectCatalog(@PathVariable Long id, @Valid @RequestBody ModerationRequest r) {
        CatalogProduct c = service.reviewCatalog(id, false, r.reason());
        return ApiResponse.success("Katalog ürünü reddedildi.", new CatalogView(c.getId(), c.getName(), c.getDescription(), c.getCategory(), c.getBrand(), c.getOemNumber(), c.getModerationStatus().name(), c.getActive(), List.of(), List.of()));
    }

    @PutMapping("/api/admin/catalog/{id}/active")
    public ApiResponse<CatalogView> activeCatalog(@PathVariable Long id, @Valid @RequestBody ProductActiveRequest request) {
        CatalogProduct c = service.setCatalogActive(id, request.getActive());
        return ApiResponse.success("Catalog visibility updated.", new CatalogView(c.getId(), c.getName(), c.getDescription(), c.getCategory(), c.getBrand(), c.getOemNumber(), c.getModerationStatus().name(), c.getActive(), List.of(), List.of()));
    }

    @GetMapping("/api/me/favorites")
    public ApiResponse<List<CatalogView>> favorites(Authentication a) {
        return ApiResponse.success("Favoriler.", service.myFavorites(a.getName()));
    }

    @PostMapping("/api/me/favorites/{catalogId}")
    public ApiResponse<Void> favorite(Authentication a, @PathVariable Long catalogId) {
        service.favorite(a.getName(), catalogId);
        return ApiResponse.success("Favoriye eklendi.", null);
    }

    @DeleteMapping("/api/me/favorites/{catalogId}")
    public ApiResponse<Void> unfavorite(Authentication a, @PathVariable Long catalogId) {
        service.unfavorite(a.getName(), catalogId);
        return ApiResponse.success("Favoriden çıkarıldı.", null);
    }

    @PostMapping("/api/complaints")
    public ResponseEntity<ApiResponse<ComplaintView>> complaint(Authentication a, @Valid @RequestBody ComplaintCreateRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Şikâyet alındı.", service.createComplaint(a.getName(), r)));
    }

    @GetMapping("/api/admin/complaints")
    public ApiResponse<Page<ComplaintView>> complaints(@RequestParam(required = false) ComplaintStatus status, @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Şikâyetler.", service.adminComplaints(status, page, size));
    }

    @PutMapping("/api/admin/complaints/{id}")
    public ApiResponse<ComplaintView> resolve(@PathVariable Long id, @RequestParam ComplaintStatus status, @RequestParam(required = false) String note) {
        return ApiResponse.success("Şikâyet güncellendi.", service.resolveComplaint(id, status, note));
    }
}
