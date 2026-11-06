package com.parca.yedekparca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VehicleModelCreateRequest(@NotNull Long brandId, @NotBlank @Size(max = 180) String name,
                                        @NotEmpty List<@NotNull Long> vehicleTypeIds) {
}
