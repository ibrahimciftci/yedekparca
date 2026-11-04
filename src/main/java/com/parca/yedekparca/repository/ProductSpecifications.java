package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.Product;
import com.parca.yedekparca.entity.ProductModerationStatus;
import com.parca.yedekparca.entity.ProductVehicleCompatibility;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> forAdmin(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addProductSearch(predicates, root, cb, criteria.search());
            addEqualsIgnoreCase(predicates, root, cb, "category", criteria.category());
            addEqualsIgnoreCase(predicates, root, cb, "brand", criteria.brand());
            if (criteria.sellerId() != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), criteria.sellerId()));
            }
            if (criteria.moderationStatus() != null) {
                predicates.add(cb.equal(root.get("moderationStatus"), criteria.moderationStatus()));
            }
            if (criteria.active() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.active()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Product> forSeller(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addProductSearch(predicates, root, cb, criteria.search());
            if (criteria.sellerId() != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), criteria.sellerId()));
            }
            if (criteria.moderationStatus() != null) {
                predicates.add(cb.equal(root.get("moderationStatus"), criteria.moderationStatus()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Product> forCatalog(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("moderationStatus"), ProductModerationStatus.APPROVED));
            predicates.add(cb.isTrue(root.get("active")));

            addProductSearch(predicates, root, cb, criteria.search());
            addEqualsIgnoreCase(predicates, root, cb, "category", criteria.category());
            addEqualsIgnoreCase(predicates, root, cb, "brand", criteria.brand());
            addLikeIgnoreCase(predicates, root, cb, "oemNumber", criteria.oemNumber());

            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
            }
            if (criteria.condition() != null) {
                predicates.add(cb.equal(root.get("condition"), criteria.condition()));
            }

            if (hasText(criteria.vehicleBrand()) || hasText(criteria.vehicleModel())
                    || criteria.modelYear() != null || hasText(criteria.engineInfo())) {
                Subquery<Long> compatibilityQuery = query.subquery(Long.class);
                var compatibility = compatibilityQuery.from(ProductVehicleCompatibility.class);
                List<Predicate> compatibilityPredicates = new ArrayList<>();
                compatibilityPredicates.add(cb.equal(compatibility.get("product").get("id"), root.get("id")));
                addEqualsIgnoreCase(compatibilityPredicates, compatibility, cb, "vehicleBrand", criteria.vehicleBrand());
                addEqualsIgnoreCase(compatibilityPredicates, compatibility, cb, "vehicleModel", criteria.vehicleModel());
                addLikeIgnoreCase(compatibilityPredicates, compatibility, cb, "engineInfo", criteria.engineInfo());
                if (criteria.modelYear() != null) {
                    compatibilityPredicates.add(cb.lessThanOrEqualTo(compatibility.get("yearFrom"), criteria.modelYear()));
                    compatibilityPredicates.add(cb.greaterThanOrEqualTo(compatibility.get("yearTo"), criteria.modelYear()));
                }
                compatibilityQuery.select(compatibility.get("id"))
                        .where(cb.and(compatibilityPredicates.toArray(Predicate[]::new)));
                predicates.add(cb.exists(compatibilityQuery));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static <R> void addProductSearch(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<R> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String search) {
        if (!hasText(search)) return;
        String pattern = contains(search);
        predicates.add(cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("oemNumber")), pattern),
                cb.like(cb.lower(root.get("brand")), pattern)
        ));
    }

    private static <R> void addEqualsIgnoreCase(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<R> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String field,
            String value) {
        if (hasText(value)) {
            predicates.add(cb.equal(cb.lower(root.get(field)), value.trim().toLowerCase(Locale.ROOT)));
        }
    }

    private static <R> void addEqualsIgnoreCase(
            List<Predicate> predicates,
            jakarta.persistence.criteria.From<?, R> from,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String field,
            String value) {
        if (hasText(value)) {
            predicates.add(cb.equal(cb.lower(from.get(field)), value.trim().toLowerCase(Locale.ROOT)));
        }
    }

    private static <R> void addLikeIgnoreCase(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<R> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String field,
            String value) {
        if (hasText(value)) {
            predicates.add(cb.like(cb.lower(root.get(field)), contains(value)));
        }
    }

    private static <R> void addLikeIgnoreCase(
            List<Predicate> predicates,
            jakarta.persistence.criteria.From<?, R> from,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String field,
            String value) {
        if (hasText(value)) {
            predicates.add(cb.like(cb.lower(from.get(field)), contains(value)));
        }
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
