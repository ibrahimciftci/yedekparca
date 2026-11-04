package com.parca.yedekparca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerApplicationRequest {

    @NotBlank(message = "Mağaza adı boş bırakılamaz.")
    @Size(max = 150)
    private String storeName;

    @NotBlank(message = "Telefon numarası boş bırakılamaz.")
    @Size(max = 20)
    private String phone;

    @NotBlank(message = "Şehir boş bırakılamaz.")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "İlçe boş bırakılamaz.")
    @Size(max = 100)
    private String district;

    @Size(max = 1000)
    private String description;
}