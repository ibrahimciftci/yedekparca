package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findAllByCityId(Long cityId);
    Optional<District> findByIdAndCityId(Long id, Long cityId);
}
