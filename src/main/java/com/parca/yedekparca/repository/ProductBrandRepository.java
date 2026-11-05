package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.ProductBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductBrandRepository extends JpaRepository<ProductBrand, Long> {
    List<ProductBrand> findAllByActiveTrueOrderByNameAsc();
    Optional<ProductBrand> findByNameIgnoreCaseAndActiveTrue(String name);
}
