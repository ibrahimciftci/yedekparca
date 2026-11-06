package com.parca.yedekparca.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductVehicleCompatibilityResponse {

    private Long id;
    private Long vehicleModelId;
    private Long vehicleBrandId;
    private List<Long> vehicleTypeIds;
    private String vehicleBrand;
    private String vehicleModel;
    private Integer yearFrom;
    private Integer yearTo;
    private String engineInfo;
}
