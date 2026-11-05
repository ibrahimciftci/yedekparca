package com.parca.yedekparca.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "offer_photos", indexes = @Index(name = "idx_offer_photo_order", columnList = "offer_id,display_order"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private ProductOffer offer;
    @Column(name = "object_key", nullable = false, unique = true, length = 500)
    private String objectKey;
    @Column(name = "public_url", length = 2048)
    private String publicUrl;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Column(name = "primary_photo", nullable = false)
    private Boolean primaryPhoto;
}
