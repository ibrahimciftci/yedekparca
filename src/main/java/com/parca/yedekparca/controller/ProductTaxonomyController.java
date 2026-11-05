package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.ApiResponse;
import com.parca.yedekparca.entity.ProductBrand;
import com.parca.yedekparca.entity.ProductCategory;
import com.parca.yedekparca.repository.ProductBrandRepository;
import com.parca.yedekparca.repository.ProductCategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductTaxonomyController {
    private final ProductCategoryRepository categories;
    private final ProductBrandRepository brands;

    public ProductTaxonomyController(ProductCategoryRepository categories, ProductBrandRepository brands) {
        this.categories = categories;
        this.brands = brands;
    }

    @GetMapping("/api/catalog/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.success("Kategoriler.", categories.findAllByActiveTrueOrderByNameAsc().stream().map(ProductCategory::getName).toList());
    }

    @GetMapping("/api/catalog/brands")
    public ApiResponse<List<String>> brands() {
        return ApiResponse.success("Markalar.", brands.findAllByActiveTrueOrderByNameAsc().stream().map(ProductBrand::getName).toList());
    }
}
