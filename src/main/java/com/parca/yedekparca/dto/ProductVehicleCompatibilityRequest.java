package com.parca.yedekparca.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVehicleCompatibilityRequest {

    @Size(max = 100)
    private String vehicleBrand;

    @Size(max = 100)
    private String vehicleModel;

    private Long vehicleModelId;

    @NotNull
    @Min(1886)
    private Integer yearFrom;

    @NotNull
    @Min(1886)
    private Integer yearTo;

    @Size(max = 150)
    private String engineInfo;
}
