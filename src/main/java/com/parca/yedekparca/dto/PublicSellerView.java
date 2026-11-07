package com.parca.yedekparca.dto;

public record PublicSellerView(Long id, String storeName, String city, String district, Long cityId, Long districtId,
                               String description, String logoUrl, long offerCount) { }
