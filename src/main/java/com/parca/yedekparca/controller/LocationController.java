package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.ApiResponse;
import com.parca.yedekparca.dto.CityView;
import com.parca.yedekparca.dto.DistrictView;
import com.parca.yedekparca.service.LocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LocationController {
    private final LocationService locations;

    public LocationController(LocationService locations) { this.locations = locations; }

    @GetMapping("/api/locations/cities")
    public ApiResponse<List<CityView>> cities() {
        return ApiResponse.success("İller.", locations.cities());
    }

    @GetMapping("/api/locations/cities/{cityId}/districts")
    public ApiResponse<List<DistrictView>> districts(@PathVariable Long cityId) {
        return ApiResponse.success("İlçeler.", locations.districts(cityId));
    }
}
