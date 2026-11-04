package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.ApiResponse;
import com.parca.yedekparca.dto.ProductCreateRequest;
import com.parca.yedekparca.dto.ProductPageResponse;
import com.parca.yedekparca.dto.ProductResponse;
import com.parca.yedekparca.dto.ProductUpdateRequest;
import com.parca.yedekparca.entity.ProductModerationStatus;
import com.parca.yedekparca.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

    private final ProductService productService;

    public SellerProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            Authentication authentication,
            @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Ürün inceleme için gönderildi.",
                productService.createSellerProduct(authentication.getName(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProductPageResponse<ProductResponse>>> getMine(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProductModerationStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success("Ürünler getirildi.",
                productService.getSellerProducts(authentication.getName(), search, status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getOne(
            Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Ürün detayı getirildi.",
                productService.getSellerProduct(authentication.getName(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ürün güncellendi.",
                productService.updateSellerProduct(authentication.getName(), id, request)));
    }
}
