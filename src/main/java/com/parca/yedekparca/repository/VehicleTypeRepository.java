package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    List<VehicleType> findAllByActiveTrueOrderByNormalizedNameAsc();
    Optional<VehicleType> findByCode(String code);
    boolean existsByCodeIgnoreCase(String code);
}
