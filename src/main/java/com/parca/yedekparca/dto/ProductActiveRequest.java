package com.parca.yedekparca.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductActiveRequest {

    @NotNull
    private Boolean active;
}
