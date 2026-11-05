package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {
    long countByReporterIdAndSellerIdAndCreatedAtAfter(Long userId, Long sellerId, java.time.LocalDateTime after);

    boolean existsByReporterIdAndOfferIdAndCreatedAtAfter(Long userId, Long offerId, java.time.LocalDateTime after);
}
