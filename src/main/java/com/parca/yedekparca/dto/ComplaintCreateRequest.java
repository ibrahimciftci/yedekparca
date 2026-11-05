package com.parca.yedekparca.dto;

import com.parca.yedekparca.entity.ComplaintCategory;
import jakarta.validation.constraints.*;

public record ComplaintCreateRequest(@NotNull Long sellerId, Long offerId, @NotNull ComplaintCategory category,
                                     @NotBlank @Size(max = 2000) String description) {
}
