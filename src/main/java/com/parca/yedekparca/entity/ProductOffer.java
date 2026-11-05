package com.parca.yedekparca.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_offers", uniqueConstraints = @UniqueConstraint(name = "uk_offer_seller_catalog_condition", columnNames = {"seller_id", "catalog_product_id", "product_condition"}), indexes = {@Index(name = "idx_offer_catalog_public", columnList = "catalog_product_id,moderation_status,active,price"), @Index(name = "idx_offer_seller_created", columnList = "seller_id,created_at")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_product_id", nullable = false)
    private CatalogProduct catalogProduct;
    @Column(name = "legacy_product_id", unique = true)
    private Long legacyProductId;
    @Column(name = "offer_description", length = 4000)
    private String offerDescription;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;
    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition", nullable = false, length = 10)
    private ProductCondition condition;
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    @Builder.Default
    private ProductModerationStatus moderationStatus = ProductModerationStatus.PENDING;
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    @Column(name = "price_updated_at", nullable = false)
    private LocalDateTime priceUpdatedAt;
    @Column(name = "stock_updated_at", nullable = false)
    private LocalDateTime stockUpdatedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private java.util.List<OfferPhoto> photos = new java.util.ArrayList<>();

    @PrePersist
    void create() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (priceUpdatedAt == null) priceUpdatedAt = now;
        if (stockUpdatedAt == null) stockUpdatedAt = now;
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }
}
