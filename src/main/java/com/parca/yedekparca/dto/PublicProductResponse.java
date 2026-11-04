package com.parca.yedekparca.dto;

import com.parca.yedekparca.entity.ProductCondition;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PublicProductResponse {

    private Long id;
    private String sellerStoreName;
    private String name;
    private String description;
    private String category;
    private String brand;
    private String oemNumber;
    private BigDecimal price;
    private Integer stockQuantity;
    private ProductCondition condition;
    private String imageUrl;
    private List<ProductVehicleCompatibilityResponse> vehicleCompatibilities;
    private LocalDateTime createdAt;
}
