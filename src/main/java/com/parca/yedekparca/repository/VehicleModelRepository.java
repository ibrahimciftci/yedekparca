package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.VehicleModel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    @EntityGraph(attributePaths = {"brand", "types"})
    List<VehicleModel> findAllByBrandIdAndActiveTrueOrderByNormalizedNameAsc(Long brandId);

    @Query("select distinct m from VehicleModel m join m.types t where m.brand.id = :brandId and t.id = :typeId and m.active = true order by m.normalizedName")
    @EntityGraph(attributePaths = {"brand", "types"})
    List<VehicleModel> findActiveByBrandAndType(@Param("brandId") Long brandId, @Param("typeId") Long typeId);

    boolean existsByBrandIdAndNormalizedName(Long brandId, String normalizedName);
}
