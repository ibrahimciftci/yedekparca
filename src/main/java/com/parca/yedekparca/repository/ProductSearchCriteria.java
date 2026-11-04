package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.ProductCondition;
import com.parca.yedekparca.entity.ProductModerationStatus;

import java.math.BigDecimal;

public record ProductSearchCriteria(
        String search,
        String category,
        String brand,
        String oemNumber,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ProductCondition condition,
        String vehicleBrand,
        String vehicleModel,
        Integer modelYear,
        String engineInfo,
        Long sellerId,
        ProductModerationStatus moderationStatus,
        Boolean active) {
}
