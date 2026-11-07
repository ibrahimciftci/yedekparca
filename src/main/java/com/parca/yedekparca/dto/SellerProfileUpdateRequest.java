package com.parca.yedekparca.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SellerProfileUpdateRequest(@NotBlank @Size(max = 150) String storeName,
                                         @NotBlank @Pattern(regexp = "[+0-9() -]{7,20}") String phone,
                                         @Pattern(regexp = "|[+0-9() -]{7,20}") String whatsappPhone,
                                         @Email @Size(max = 150) String contactEmail,
                                         @NotBlank @Size(max = 100) String city,
                                         @NotBlank @Size(max = 100) String district,
                                         @Size(max = 500) String streetAddress,
                                         @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
                                         @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
                                         String workingHours, @Size(max = 1000) String description,
                                         @Pattern(regexp = "|https?://.{1,2040}") String websiteUrl,
                                         @Pattern(regexp = "|https?://.{1,2040}") String logoUrl,
                                         Long cityId, Long districtId) {
}
