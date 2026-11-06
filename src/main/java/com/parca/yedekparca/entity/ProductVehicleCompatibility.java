package com.parca.yedekparca.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_vehicle_compatibilities", indexes = {
        @Index(name = "idx_vehicle_compat_product", columnList = "product_id"),
        @Index(name = "idx_vehicle_compat_vehicle", columnList = "vehicle_brand, vehicle_model, year_from, year_to")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVehicleCompatibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_product_id")
    private CatalogProduct catalogProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_model_id")
    private VehicleModel model;

    @Column(name = "vehicle_brand", nullable = false, length = 100)
    private String vehicleBrand;

    @Column(name = "vehicle_model", nullable = false, length = 100)
    private String vehicleModel;

    @Column(name = "year_from", nullable = false)
    private Integer yearFrom;

    @Column(name = "year_to", nullable = false)
    private Integer yearTo;

    @Column(length = 150)
    private String engineInfo;
}
