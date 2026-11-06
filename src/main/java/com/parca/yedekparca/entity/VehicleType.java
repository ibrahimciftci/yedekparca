package com.parca.yedekparca.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicle_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "normalized_name", nullable = false, unique = true, length = 100)
    private String normalizedName;
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    @ManyToMany(mappedBy = "types")
    @Builder.Default
    private List<VehicleModel> models = new ArrayList<>();
}
