package com.parca.yedekparca.repository;

import com.parca.yedekparca.entity.VehicleBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Long> {
    List<VehicleBrand> findAllByActiveTrueOrderByNormalizedNameAsc();

    @Query("select distinct b from VehicleBrand b join b.models m join m.types t where t.id = :typeId and b.active = true and m.active = true order by b.normalizedName")
    List<VehicleBrand> findActiveForType(@Param("typeId") Long typeId);

    boolean existsByNormalizedName(String normalizedName);
}
