package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;

import java.util.*;

public final class MarketplaceSpecifications {
    private MarketplaceSpecifications() {
    }

    public static Specification<ProductOffer> offers(String search, ProductCondition condition, Long sellerId, ProductModerationStatus status, Boolean active, boolean publicOnly) {
        return (r, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (publicOnly) {
                p.add(cb.equal(r.get("moderationStatus"), ProductModerationStatus.APPROVED));
                p.add(cb.isTrue(r.get("active")));
                p.add(cb.equal(r.get("catalogProduct").get("moderationStatus"), CatalogModerationStatus.APPROVED));
                p.add(cb.isTrue(r.get("catalogProduct").get("active")));
                p.add(cb.equal(r.get("seller").get("status"), SellerStatus.APPROVED));
                p.add(cb.isTrue(r.get("seller").get("profileActive")));
                p.add(cb.isTrue(r.get("seller").get("user").get("active")));
            }
            if (search != null && !search.isBlank()) {
                String s = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(cb.like(cb.lower(r.get("catalogProduct").get("name")), s), cb.like(cb.lower(r.get("catalogProduct").get("brand")), s), cb.like(cb.lower(r.get("catalogProduct").get("oemNumber")), s)));
            }
            if (condition != null) p.add(cb.equal(r.get("condition"), condition));
            if (sellerId != null) p.add(cb.equal(r.get("seller").get("id"), sellerId));
            if (status != null) p.add(cb.equal(r.get("moderationStatus"), status));
            if (active != null) p.add(cb.equal(r.get("active"), active));
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
