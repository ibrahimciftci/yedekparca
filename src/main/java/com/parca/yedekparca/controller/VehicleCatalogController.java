package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.*;
import com.parca.yedekparca.service.VehicleCatalogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class VehicleCatalogController {
    private final VehicleCatalogService service;

    public VehicleCatalogController(VehicleCatalogService service) {
        this.service = service;
    }

    @GetMapping("/api/vehicles/types")
    public ApiResponse<List<VehicleTypeView>> types() {
        return ApiResponse.success("Araç türleri.", service.listTypes());
    }

    @GetMapping("/api/vehicles/brands")
    public ApiResponse<List<VehicleBrandView>> brands(@RequestParam(required = false) Long typeId) {
        return ApiResponse.success("Araç markaları.", service.listBrands(typeId));
    }

    @GetMapping("/api/vehicles/brands/{brandId}/models")
    public ApiResponse<List<VehicleModelView>> models(@PathVariable Long brandId,
                                                       @RequestParam(required = false) Long typeId) {
        return ApiResponse.success("Araç modelleri.", service.listModels(brandId, typeId));
    }

    @PostMapping("/api/admin/vehicles/types")
    public ApiResponse<VehicleTypeView> createType(@Valid @RequestBody VehicleTypeCreateRequest request) {
        return ApiResponse.success("Araç türü eklendi.", service.createType(request));
    }

    @PostMapping("/api/admin/vehicles/brands")
    public ApiResponse<VehicleBrandView> createBrand(@Valid @RequestBody VehicleBrandCreateRequest request) {
        return ApiResponse.success("Araç markası eklendi.", service.createBrand(request));
    }

    @PostMapping("/api/admin/vehicles/models")
    public ApiResponse<VehicleModelView> createModel(@Valid @RequestBody VehicleModelCreateRequest request) {
        return ApiResponse.success("Araç modeli eklendi.", service.createModel(request));
    }
}
