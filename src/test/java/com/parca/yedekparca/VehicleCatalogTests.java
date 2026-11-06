package com.parca.yedekparca;

import com.parca.yedekparca.service.VehicleCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VehicleCatalogTests {
    @Autowired private VehicleCatalogService vehicles;
    @Autowired private MockMvc mvc;

    @Test
    void publicListsReturnBrandsAndModelsLinkedToTheSelectedType() {
        var type = vehicles.listTypes().stream().findFirst().orElseThrow();
        var brand = vehicles.listBrands(type.id()).stream().findFirst().orElseThrow();
        var model = vehicles.listModels(brand.id(), type.id()).stream().findFirst().orElseThrow();

        assertEquals(brand.id(), model.brandId());
        assertTrue(model.vehicleTypeIds().contains(type.id()));
        assertTrue(vehicles.listBrands(null).stream().anyMatch(item -> item.id().equals(brand.id())));
    }

    @Test
    void vehicleListsArePublicButVehicleWritesRequireAdminAuthentication() throws Exception {
        mvc.perform(get("/api/vehicles/types")).andExpect(status().isOk());
        mvc.perform(post("/api/admin/vehicles/brands").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Unauthorized Test\"}")).andExpect(status().isForbidden());
    }
}
