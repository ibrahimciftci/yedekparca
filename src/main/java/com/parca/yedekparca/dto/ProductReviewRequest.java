package com.parca.yedekparca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductReviewRequest {

    @NotBlank(message = "Ret gerekçesi zorunludur.")
    @Size(max = 500)
    private String rejectionReason;
}
