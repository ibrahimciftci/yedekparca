package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.ApiResponse;
import com.parca.yedekparca.dto.ProductPageResponse;
import com.parca.yedekparca.dto.PublicProductResponse;
import com.parca.yedekparca.entity.ProductCondition;
import com.parca.yedekparca.repository.ProductSearchCriteria;
import com.parca.yedekparca.service.ProductService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProductPageResponse<PublicProductResponse>>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String oemNumber,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxPrice,
            @RequestParam(required = false) ProductCondition condition,
            @RequestParam(required = false) String vehicleBrand,
            @RequestParam(required = false) String vehicleModel,
            @RequestParam(required = false) @Min(1886) Integer modelYear,
            @RequestParam(required = false) String engineInfo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        ProductSearchCriteria criteria = new ProductSearchCriteria(search, category, brand, oemNumber,
                minPrice, maxPrice, condition, vehicleBrand, vehicleModel, modelYear, engineInfo,
                null, null, null);
        return ResponseEntity.ok(ApiResponse.success("Ürünler getirildi.",
                productService.getPublicProducts(criteria, page, size, sortBy, direction)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Ürün detayı getirildi.",
                productService.getPublicProduct(id)));
    }
}
