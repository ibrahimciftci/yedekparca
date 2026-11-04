package com.parca.yedekparca.dto;

import com.parca.yedekparca.entity.ProductCondition;
import com.parca.yedekparca.entity.ProductModerationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private Long sellerId;
    private String sellerStoreName;
    private String sellerFirstName;
    private String sellerLastName;
    private String sellerEmail;
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
    private ProductModerationStatus moderationStatus;
    private String rejectionReason;
    private LocalDateTime reviewedAt;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
