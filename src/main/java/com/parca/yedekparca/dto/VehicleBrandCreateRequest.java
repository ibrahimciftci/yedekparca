package com.parca.yedekparca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleBrandCreateRequest(@NotBlank @Size(max = 150) String name) {
}
