package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;

import java.math.BigDecimal;
import java.util.*;

public final class CatalogSpecifications {
    private CatalogSpecifications() {
    }

    public static Specification<CatalogProduct> publicSearch(String search, String category, String brand, String oem, BigDecimal min, BigDecimal max, ProductCondition condition, String vehicleBrand, String vehicleModel, Integer year, String engine) {
        return publicSearch(search, category, brand, oem, min, max, condition, vehicleBrand, vehicleModel, year, engine, null, null, null);
    }

    public static Specification<CatalogProduct> publicSearch(String search, String category, String brand, String oem,
            BigDecimal min, BigDecimal max, ProductCondition condition, String vehicleBrand, String vehicleModel,
            Integer year, String engine, Long vehicleTypeId, Long vehicleBrandId, Long vehicleModelId) {
        return (r, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(r.get("moderationStatus"), CatalogModerationStatus.APPROVED));
            p.add(cb.isTrue(r.get("active")));
            if (text(search)) {
                String s = like(search);
                p.add(cb.or(cb.like(cb.lower(r.get("name")), s), cb.like(cb.lower(r.get("brand")), s), cb.like(cb.lower(r.get("oemNumber")), s)));
            }
            eq(p, r, cb, "category", category);
            eq(p, r, cb, "brand", brand);
            like(p, r, cb, "oemNumber", oem);
            if (min != null || max != null || condition != null) {
                Subquery<Long> sq = q.subquery(Long.class);
                Root<ProductOffer> o = sq.from(ProductOffer.class);
                List<Predicate> x = publicOffer(o, cb, r);
                if (condition != null) x.add(cb.equal(o.get("condition"), condition));
                if (min != null) x.add(cb.greaterThanOrEqualTo(o.get("price"), min));
                if (max != null) x.add(cb.lessThanOrEqualTo(o.get("price"), max));
                sq.select(o.get("id")).where(cb.and(x.toArray(Predicate[]::new)));
                p.add(cb.exists(sq));
            }
            if (text(vehicleBrand) || text(vehicleModel) || year != null || text(engine)
                    || vehicleTypeId != null || vehicleBrandId != null || vehicleModelId != null) {
                Subquery<Long> sq = q.subquery(Long.class);
                Root<ProductVehicleCompatibility> v = sq.from(ProductVehicleCompatibility.class);
                List<Predicate> x = new ArrayList<>();
                x.add(cb.equal(v.get("catalogProduct").get("id"), r.get("id")));
                if (vehicleModelId != null) x.add(cb.equal(v.get("model").get("id"), vehicleModelId));
                if (vehicleBrandId != null) x.add(cb.equal(v.get("model").get("brand").get("id"), vehicleBrandId));
                if (vehicleTypeId != null) x.add(cb.equal(v.join("model").join("types").get("id"), vehicleTypeId));
                eq(x, v, cb, "vehicleBrand", vehicleBrand);
                eq(x, v, cb, "vehicleModel", vehicleModel);
                like(x, v, cb, "engineInfo", engine);
                if (year != null) {
                    x.add(cb.lessThanOrEqualTo(v.get("yearFrom"), year));
                    x.add(cb.greaterThanOrEqualTo(v.get("yearTo"), year));
                }
                sq.select(v.get("id")).where(cb.and(x.toArray(Predicate[]::new)));
                p.add(cb.exists(sq));
            }
            Subquery<Long> visible = q.subquery(Long.class);
            Root<ProductOffer> o = visible.from(ProductOffer.class);
            visible.select(o.get("id")).where(cb.and(publicOffer(o, cb, r).toArray(Predicate[]::new)));
            p.add(cb.exists(visible));
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static List<Predicate> publicOffer(Root<ProductOffer> o, CriteriaBuilder cb, Root<CatalogProduct> c) {
        return new ArrayList<>(List.of(cb.equal(o.get("catalogProduct").get("id"), c.get("id")), cb.equal(o.get("moderationStatus"), ProductModerationStatus.APPROVED), cb.isTrue(o.get("active")), cb.equal(o.get("seller").get("status"), SellerStatus.APPROVED), cb.isTrue(o.get("seller").get("profileActive")), cb.isTrue(o.get("seller").get("user").get("active"))));
    }

    private static void eq(List<Predicate> p, Root<?> r, CriteriaBuilder cb, String f, String v) {
        if (text(v)) p.add(cb.equal(cb.lower(r.get(f)), v.trim().toLowerCase(Locale.ROOT)));
    }

    private static void eq(List<Predicate> p, From<?, ?> r, CriteriaBuilder cb, String f, String v) {
        if (text(v)) p.add(cb.equal(cb.lower(r.get(f)), v.trim().toLowerCase(Locale.ROOT)));
    }

    private static void like(List<Predicate> p, Root<?> r, CriteriaBuilder cb, String f, String v) {
        if (text(v)) p.add(cb.like(cb.lower(r.get(f)), like(v)));
    }

    private static void like(List<Predicate> p, From<?, ?> r, CriteriaBuilder cb, String f, String v) {
        if (text(v)) p.add(cb.like(cb.lower(r.get(f)), like(v)));
    }

    private static String like(String s) {
        return "%" + s.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static boolean text(String s) {
        return s != null && !s.isBlank();
    }
}
