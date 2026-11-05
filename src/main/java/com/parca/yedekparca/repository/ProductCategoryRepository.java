package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findAllByActiveTrueOrderByNameAsc();
    Optional<ProductCategory> findByNameIgnoreCaseAndActiveTrue(String name);
}
