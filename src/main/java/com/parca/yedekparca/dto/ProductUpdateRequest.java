package com.parca.yedekparca.dto;

import com.parca.yedekparca.entity.ProductCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductUpdateRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 4000)
    private String description;

    @NotBlank
    @Size(max = 100)
    private String category;

    @NotBlank
    @Size(max = 100)
    private String brand;

    @Size(max = 100)
    private String oemNumber;

    @NotNull
    @DecimalMin(value = "0.01")
    @DecimalMax(value = "9999999999.99")
    private BigDecimal price;

    @NotNull
    @Min(0)
    private Integer stockQuantity;

    @NotNull
    private ProductCondition condition;

    @Size(max = 2048)
    private String imageUrl;

    @Valid
    private List<ProductVehicleCompatibilityRequest> vehicleCompatibilities;
}
