package com.parca.yedekparca.dto;

import java.math.BigDecimal;

public record SellerProfileView(Long id, String storeName, String phone, String whatsappPhone, String contactEmail,
                                String city, String district, String streetAddress, BigDecimal latitude,
                                BigDecimal longitude, String workingHours, String description, String websiteUrl,
                                String logoUrl, Boolean profileActive) {
}
