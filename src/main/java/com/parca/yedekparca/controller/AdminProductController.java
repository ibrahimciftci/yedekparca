package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.*;
import com.parca.yedekparca.entity.ProductModerationStatus;
import com.parca.yedekparca.repository.ProductSearchCriteria;
import com.parca.yedekparca.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProductPageResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) ProductModerationStatus moderationStatus,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        ProductSearchCriteria criteria = new ProductSearchCriteria(search, category, brand, null,
                null, null, null, null, null, null, null, sellerId, moderationStatus, active);
        return ResponseEntity.ok(ApiResponse.success("Ürünler getirildi.",
                productService.getAdminProducts(criteria, page, size, sortBy, direction)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Ürün detayı getirildi.",
                productService.getAdminProduct(id)));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ProductResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Ürün onaylandı.",
                productService.approveProduct(id)));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ProductResponse>> reject(
            @PathVariable Long id, @Valid @RequestBody ProductReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ürün reddedildi.",
                productService.rejectProduct(id, request.getRejectionReason())));
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<ApiResponse<ProductResponse>> updateActive(
            @PathVariable Long id, @Valid @RequestBody ProductActiveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ürün aktiflik durumu güncellendi.",
                productService.updateActiveStatus(id, request.getActive())));
    }
}
