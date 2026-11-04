package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.Seller;
import com.parca.yedekparca.entity.SellerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Seller> findByStatus(SellerStatus status);
}