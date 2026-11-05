package com.parca.yedekparca.dto;

import java.util.List;

public record CatalogView(Long id, String name, String description, String category, String brand, String oemNumber,
                          String moderationStatus, Boolean active,
                          List<ProductVehicleCompatibilityResponse> vehicleCompatibilities, List<OfferView> offers) {
}
