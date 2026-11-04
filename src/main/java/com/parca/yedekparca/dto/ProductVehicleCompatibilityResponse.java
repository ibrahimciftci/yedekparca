package com.parca.yedekparca.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductVehicleCompatibilityResponse {

    private Long id;
    private String vehicleBrand;
    private String vehicleModel;
    private Integer yearFrom;
    private Integer yearTo;
    private String engineInfo;
}
