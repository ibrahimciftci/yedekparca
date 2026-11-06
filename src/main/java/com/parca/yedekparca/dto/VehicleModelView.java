package com.parca.yedekparca.dto;

import java.util.List;

public record VehicleModelView(Long id, Long brandId, String brandName, String name,
                               List<Long> vehicleTypeIds, List<String> vehicleTypeNames) {
}
