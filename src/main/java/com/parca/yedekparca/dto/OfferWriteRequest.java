package com.parca.yedekparca.dto;

import com.parca.yedekparca.entity.ProductCondition;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record OfferWriteRequest(Long catalogProductId, @NotBlank @Size(max = 200) String name,
                                @Size(max = 4000) String description, @NotBlank @Size(max = 100) String category,
                                @NotBlank @Size(max = 100) String brand, @Size(max = 100) String oemNumber,
                                @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
                                @NotNull @Min(0) Integer stockQuantity, @NotNull ProductCondition condition,
                                List<ProductVehicleCompatibilityRequest> vehicleCompatibilities,
                                @Size(max = 4000) String offerDescription) {
    public OfferWriteRequest(Long catalogProductId, String name, String description, String category, String brand,
                             String oemNumber, BigDecimal price, Integer stockQuantity, ProductCondition condition,
                             List<ProductVehicleCompatibilityRequest> vehicleCompatibilities) {
        this(catalogProductId, name, description, category, brand, oemNumber, price, stockQuantity, condition,
                vehicleCompatibilities, null);
    }
}
