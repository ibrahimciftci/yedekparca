package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.*;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, Long>, JpaSpecificationExecutor<CatalogProduct> {
}
