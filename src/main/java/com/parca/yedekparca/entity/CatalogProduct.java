package com.parca.yedekparca.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_products", indexes = {@Index(name = "idx_catalog_status_name", columnList = "moderation_status,name"), @Index(name = "idx_catalog_oem", columnList = "oem_number")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(length = 4000)
    private String description;
    @Column(nullable = false, length = 100)
    private String category;
    @Column(nullable = false, length = 100)
    private String brand;
    @Column(name = "oem_number", length = 100)
    private String oemNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    @Builder.Default
    private CatalogModerationStatus moderationStatus = CatalogModerationStatus.PENDING;
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "catalogProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<ProductVehicleCompatibility> vehicleCompatibilities = new java.util.ArrayList<>();

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }
}
