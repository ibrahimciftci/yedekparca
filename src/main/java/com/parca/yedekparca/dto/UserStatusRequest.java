package com.parca.yedekparca.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusRequest {

    @NotNull(message = "Aktiflik durumu boş bırakılamaz.")
    private Boolean active;
}
