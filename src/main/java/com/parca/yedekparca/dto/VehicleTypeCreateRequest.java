package com.parca.yedekparca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleTypeCreateRequest(@NotBlank @Size(max = 30) String code,
                                       @NotBlank @Size(max = 100) String name) {
}
