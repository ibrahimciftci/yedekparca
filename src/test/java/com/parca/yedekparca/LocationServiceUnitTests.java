package com.parca.yedekparca;

import com.parca.yedekparca.entity.City;
import com.parca.yedekparca.entity.District;
import com.parca.yedekparca.repository.CityRepository;
import com.parca.yedekparca.repository.DistrictRepository;
import com.parca.yedekparca.service.LocationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocationServiceUnitTests {
    private final CityRepository cities = mock(CityRepository.class);
    private final DistrictRepository districts = mock(DistrictRepository.class);
    private final LocationService service = new LocationService(cities, districts);

    @Test
    void resolvesLegacyAsciiNamesToCanonicalTurkishLocations() {
        City istanbul = mock(City.class);
        District kadikoy = mock(District.class);
        when(istanbul.getId()).thenReturn(34L);
        when(istanbul.getName()).thenReturn("İstanbul");
        when(kadikoy.getId()).thenReturn(3401L);
        when(kadikoy.getName()).thenReturn("Kadıköy");
        when(cities.findAll()).thenReturn(List.of(istanbul));
        when(districts.findAllByCityId(34L)).thenReturn(List.of(kadikoy));

        var resolved = service.resolve(null, null, "Istanbul", "Kadikoy");

        assertEquals(34L, resolved.city().getId());
        assertEquals(3401L, resolved.district().getId());
    }

    @Test
    void rejectsDistrictThatDoesNotBelongToSelectedCity() {
        when(cities.findById(34L)).thenReturn(Optional.of(mock(City.class)));
        when(districts.findByIdAndCityId(6L, 34L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.resolve(34L, 6L, "İstanbul", "Çankaya"));
    }

    @Test
    void districtFilterRequiresItsParentCity() {
        assertThrows(IllegalArgumentException.class, () -> service.validateFilter(null, 6L));
    }
}
