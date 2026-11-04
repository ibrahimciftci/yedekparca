package com.parca.yedekparca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerReviewRequest {

    @NotBlank(message = "Ret nedeni boş bırakılamaz.")
    @Size(max = 500, message = "Ret nedeni en fazla 500 karakter olabilir.")
    private String rejectionReason;
}