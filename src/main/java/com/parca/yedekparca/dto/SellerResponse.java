package com.parca.yedekparca.dto;

import com.parca.yedekparca.entity.SellerStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SellerResponse {

    private Long id;
    private String applicantFirstName;
    private String applicantLastName;
    private String applicantEmail;
    private String storeName;
    private String phone;
    private String city;
    private String district;
    private Long cityId;
    private Long districtId;
    private String description;
    private SellerStatus status;
    private String rejectionReason;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
}
