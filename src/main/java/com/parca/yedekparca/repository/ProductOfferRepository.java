package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ProductOfferRepository extends JpaRepository<ProductOffer, Long>, JpaSpecificationExecutor<ProductOffer> {
    Optional<ProductOffer> findByIdAndSellerId(Long id, Long sellerId);

    boolean existsBySellerIdAndCatalogProductIdAndCondition(Long sellerId, Long catalogId, ProductCondition condition);

    List<ProductOffer> findByCatalogProductIdAndModerationStatusAndActiveTrueOrderByPriceAsc(Long catalogId, ProductModerationStatus status);

    @Query("select o from ProductOffer o join fetch o.seller s join fetch o.catalogProduct c where c.id=:catalogId and o.moderationStatus=com.parca.yedekparca.entity.ProductModerationStatus.APPROVED and o.active=true and c.moderationStatus=com.parca.yedekparca.entity.CatalogModerationStatus.APPROVED and c.active=true and s.status=com.parca.yedekparca.entity.SellerStatus.APPROVED and s.profileActive=true and s.user.active=true order by o.price asc")
    List<ProductOffer> findPublicByCatalog(@Param("catalogId") Long catalogId);
}
