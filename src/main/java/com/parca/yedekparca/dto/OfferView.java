package com.parca.yedekparca.dto;

import com.parca.yedekparca.entity.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OfferView(Long id, Long catalogProductId, String name, String description, String offerDescription,
                        String category, String brand, String oemNumber, BigDecimal price, Integer stockQuantity,
                        ProductCondition condition, ProductModerationStatus moderationStatus, String rejectionReason,
                        Boolean active, LocalDateTime priceUpdatedAt, LocalDateTime stockUpdatedAt,
                        String legacyImageUrl, List<OfferPhotoView> photos,
                        List<ProductVehicleCompatibilityResponse> vehicleCompatibilities,
                        Long sellerId, String sellerName,
                        String sellerPhone, String whatsappPhone, String city, String district, String sellerLogo,
                        java.math.BigDecimal latitude, java.math.BigDecimal longitude, String streetAddress,
                        LocalDateTime createdAt) {
}
