package com.parca.yedekparca.service;

import com.parca.yedekparca.dto.CityView;
import com.parca.yedekparca.dto.DistrictView;
import com.parca.yedekparca.entity.City;
import com.parca.yedekparca.entity.District;
import com.parca.yedekparca.repository.CityRepository;
import com.parca.yedekparca.repository.DistrictRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class LocationService {
    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");
    private final CityRepository cities;
    private final DistrictRepository districts;

    public LocationService(CityRepository cities, DistrictRepository districts) {
        this.cities = cities;
        this.districts = districts;
    }

    @Transactional(readOnly = true)
    public List<CityView> cities() {
        Collator collator = Collator.getInstance(TURKISH);
        return cities.findAll().stream()
                .sorted(Comparator.comparing(City::getName, collator))
                .map(city -> new CityView(city.getId(), city.getName(), city.getPlateCode()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DistrictView> districts(Long cityId) {
        if (!cities.existsById(cityId)) throw new IllegalArgumentException("İl bulunamadı.");
        Collator collator = Collator.getInstance(TURKISH);
        return districts.findAllByCityId(cityId).stream()
                .sorted(Comparator.comparing(District::getName, collator))
                .map(district -> new DistrictView(district.getId(), district.getCity().getId(), district.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationSelection resolve(Long cityId, Long districtId, String cityName, String districtName) {
        City city;
        District district;
        if (cityId != null || districtId != null) {
            if (cityId == null || districtId == null) throw new IllegalArgumentException("İl ve ilçe birlikte seçilmelidir.");
            city = cities.findById(cityId).orElseThrow(() -> new IllegalArgumentException("Seçilen il bulunamadı."));
            district = districts.findByIdAndCityId(districtId, cityId)
                    .orElseThrow(() -> new IllegalArgumentException("Seçilen ilçe, seçilen ile bağlı değil."));
            if ((cityName != null && !cityName.isBlank() && !same(cityName, city.getName()))
                    || (districtName != null && !districtName.isBlank() && !same(districtName, district.getName()))) {
                throw new IllegalArgumentException("İl ve ilçe bilgileri seçilen kayıtlarla uyuşmuyor.");
            }
            return new LocationSelection(city, district);
        }

        String normalizedCity = normalize(cityName);
        city = cities.findAll().stream().filter(item -> normalize(item.getName()).equals(normalizedCity)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Geçerli bir il seçin."));
        String normalizedDistrict = normalize(districtName);
        district = districts.findAllByCityId(city.getId()).stream()
                .filter(item -> normalize(item.getName()).equals(normalizedDistrict)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Seçilen ilçenin bu ile bağlı olduğundan emin olun."));
        return new LocationSelection(city, district);
    }

    @Transactional(readOnly = true)
    public void validateFilter(Long cityId, Long districtId) {
        if (cityId != null && !cities.existsById(cityId)) throw new IllegalArgumentException("İl filtresi geçersiz.");
        if (districtId != null && (cityId == null || districts.findByIdAndCityId(districtId, cityId).isEmpty()))
            throw new IllegalArgumentException("İlçe filtresi seçilen il ile uyuşmuyor.");
    }

    private static boolean same(String left, String right) { return normalize(left).equals(normalize(right)); }

    private static String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").replace('ı', 'i').toLowerCase(Locale.ROOT);
    }

    public record LocationSelection(City city, District district) { }
}
