package com.parca.yedekparca.service;

import com.parca.yedekparca.dto.*;
import com.parca.yedekparca.entity.VehicleBrand;
import com.parca.yedekparca.entity.VehicleModel;
import com.parca.yedekparca.entity.VehicleType;
import com.parca.yedekparca.repository.VehicleBrandRepository;
import com.parca.yedekparca.repository.VehicleModelRepository;
import com.parca.yedekparca.repository.VehicleTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class VehicleCatalogService {
    private final VehicleTypeRepository types;
    private final VehicleBrandRepository brands;
    private final VehicleModelRepository models;

    public VehicleCatalogService(VehicleTypeRepository types, VehicleBrandRepository brands, VehicleModelRepository models) {
        this.types = types;
        this.brands = brands;
        this.models = models;
    }

    @Transactional(readOnly = true)
    public List<VehicleTypeView> listTypes() {
        return types.findAllByActiveTrueOrderByNormalizedNameAsc().stream()
                .map(type -> new VehicleTypeView(type.getId(), type.getCode(), type.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleBrandView> listBrands(Long typeId) {
        List<VehicleBrand> result = typeId == null
                ? brands.findAllByActiveTrueOrderByNormalizedNameAsc()
                : brands.findActiveForType(typeId);
        return result.stream().map(brand -> new VehicleBrandView(brand.getId(), brand.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleModelView> listModels(Long brandId, Long typeId) {
        List<VehicleModel> result = typeId == null
                ? models.findAllByBrandIdAndActiveTrueOrderByNormalizedNameAsc(brandId)
                : models.findActiveByBrandAndType(brandId, typeId);
        return result.stream().map(this::modelView).toList();
    }

    @Transactional
    public VehicleTypeView createType(VehicleTypeCreateRequest request) {
        String code = normalize(request.code()).replace(' ', '-');
        if (types.existsByCodeIgnoreCase(code)) throw new IllegalArgumentException("Bu araç türü kodu zaten kayıtlı.");
        VehicleType type = types.save(VehicleType.builder().code(code).name(request.name().trim())
                .normalizedName(normalize(request.name())).active(true).build());
        return new VehicleTypeView(type.getId(), type.getCode(), type.getName());
    }

    @Transactional
    public VehicleBrandView createBrand(VehicleBrandCreateRequest request) {
        String name = request.name().trim();
        String normalized = normalize(name);
        if (brands.existsByNormalizedName(normalized)) throw new IllegalArgumentException("Bu araç markası zaten kayıtlı.");
        VehicleBrand brand = brands.save(VehicleBrand.builder().name(name).normalizedName(normalized).active(true).build());
        return new VehicleBrandView(brand.getId(), brand.getName());
    }

    @Transactional
    public VehicleModelView createModel(VehicleModelCreateRequest request) {
        VehicleBrand brand = brands.findById(request.brandId()).filter(VehicleBrand::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Araç markası bulunamadı veya pasif."));
        String name = request.name().trim();
        String normalized = normalize(name);
        if (models.existsByBrandIdAndNormalizedName(brand.getId(), normalized))
            throw new IllegalArgumentException("Bu marka altında aynı model zaten kayıtlı.");
        List<Long> ids = request.vehicleTypeIds().stream().distinct().toList();
        List<VehicleType> modelTypes = types.findAllById(ids);
        if (modelTypes.size() != ids.size() || modelTypes.stream().anyMatch(type -> !Boolean.TRUE.equals(type.getActive())))
            throw new IllegalArgumentException("Araç türlerinden biri bulunamadı veya pasif.");
        VehicleModel model = models.save(VehicleModel.builder().brand(brand).name(name).normalizedName(normalized)
                .types(modelTypes).active(true).build());
        return modelView(model);
    }

    private VehicleModelView modelView(VehicleModel model) {
        List<VehicleType> sortedTypes = model.getTypes().stream().sorted((a, b) -> a.getNormalizedName().compareTo(b.getNormalizedName())).toList();
        return new VehicleModelView(model.getId(), model.getBrand().getId(), model.getBrand().getName(), model.getName(),
                sortedTypes.stream().map(VehicleType::getId).toList(), sortedTypes.stream().map(VehicleType::getName).toList());
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
