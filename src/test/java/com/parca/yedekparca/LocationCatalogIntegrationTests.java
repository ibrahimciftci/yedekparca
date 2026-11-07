package com.parca.yedekparca;

import com.parca.yedekparca.service.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LocationCatalogIntegrationTests {
    @Autowired private LocationService locations;
    @Autowired private MockMvc mvc;

    @Test
    void seededCatalogContainsAllSourceCitiesAndDistricts() {
        var cities = locations.cities();
        assertEquals(81, cities.size());
        long districtCount = cities.stream().mapToLong(city -> locations.districts(city.id()).size()).sum();
        assertEquals(973, districtCount);
    }

    @Test
    void locationReadEndpointsArePublic() throws Exception {
        mvc.perform(get("/api/locations/cities")).andExpect(status().isOk());
        mvc.perform(get("/api/locations/cities/34/districts")).andExpect(status().isOk());
    }
}
