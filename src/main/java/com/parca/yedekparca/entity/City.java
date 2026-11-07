package com.parca.yedekparca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cities")
@Getter
@NoArgsConstructor
public class City {
    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "plate_code", nullable = false, unique = true, length = 2)
    private String plateCode;
}
