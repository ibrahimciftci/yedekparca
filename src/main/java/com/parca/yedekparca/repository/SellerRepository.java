package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.Seller;
import com.parca.yedekparca.entity.SellerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

public interface SellerRepository extends JpaRepository<Seller, Long>, JpaSpecificationExecutor<Seller> {

    Optional<Seller> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Seller> findByStatus(SellerStatus status);

    default Page<Seller> findPublicSellers(String name, String city, String district, Pageable pageable) {
        return findPublicSellers(name, city, district, null, null, pageable);
    }

    default Page<Seller> findPublicSellers(String name, String city, String district, Long cityId, Long districtId, Pageable pageable) {
        Specification<Seller> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("status"), SellerStatus.APPROVED));
            predicates.add(cb.isTrue(root.get("profileActive")));
            predicates.add(cb.isTrue(root.get("user").get("active")));
            if (name != null && !name.isBlank()) predicates.add(cb.like(cb.lower(root.get("storeName")), "%" + name.trim().toLowerCase(java.util.Locale.ROOT) + "%"));
            if (city != null && !city.isBlank()) predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase(java.util.Locale.ROOT)));
            if (district != null && !district.isBlank()) predicates.add(cb.equal(cb.lower(root.get("district")), district.trim().toLowerCase(java.util.Locale.ROOT)));
            if (cityId != null) predicates.add(cb.equal(root.get("cityId"), cityId));
            if (districtId != null) predicates.add(cb.equal(root.get("districtId"), districtId));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return findAll(spec, pageable);
    }
}
