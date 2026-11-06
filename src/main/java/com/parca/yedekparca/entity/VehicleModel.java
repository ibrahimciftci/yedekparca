package com.parca.yedekparca.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicle_models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private VehicleBrand brand;
    @Column(nullable = false, length = 180)
    private String name;
    @Column(name = "normalized_name", nullable = false, length = 180)
    private String normalizedName;
    @Column(name = "source_countries", length = 500)
    private String sourceCountries;
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    @ManyToMany
    @JoinTable(name = "vehicle_model_types",
            joinColumns = @JoinColumn(name = "vehicle_model_id"),
            inverseJoinColumns = @JoinColumn(name = "vehicle_type_id"))
    @Builder.Default
    private List<VehicleType> types = new ArrayList<>();
}
