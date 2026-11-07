package com.parca.yedekparca.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "seller_applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 150)
    private String storeName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "district_id")
    private Long districtId;

    @Column(length = 1000)
    private String description;

    @Column(name = "logo_url", length = 2048) private String logoUrl;
    @Column(name = "whatsapp_phone", length = 20) private String whatsappPhone;
    @Column(name = "contact_email", length = 150) private String contactEmail;
    @Column(name = "street_address", length = 500) private String streetAddress;
    @Column(precision = 9, scale = 6) private java.math.BigDecimal latitude;
    @Column(precision = 9, scale = 6) private java.math.BigDecimal longitude;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "working_hours", columnDefinition = "jsonb") private String workingHours;
    @Column(name = "website_url", length = 2048) private String websiteUrl;
    @Column(name = "profile_active", nullable = false) @Builder.Default private Boolean profileActive = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SellerStatus status = SellerStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
    }
}
