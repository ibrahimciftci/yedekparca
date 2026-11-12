package com.parca.yedekparca.controller;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Central demo-only mapping from the seeded product names to representative photos. */
public final class DemoProductPhotoCatalog {
    private static final Map<String, String> PHOTO_BY_PRODUCT_TYPE = Map.ofEntries(
            Map.entry("fren balatas", "brake-pads.jpg"),
            Map.entry("ya", "oil-filter.jpg"),
            Map.entry("hava filtresi", "air-filter.jpg"),
            Map.entry("debriyaj seti", "clutch-disc.jpg"),
            Map.entry("amortis", "shock-absorber.jpg"),
            Map.entry("far", "headlamp.jpg"),
            Map.entry("su pompas", "water-pump.jpg"),
            Map.entry("ate", "ignition-coil.jpg"),
            Map.entry("radyat", "radiator.jpg"),
            Map.entry("sal", "control-arm.jpg")
    );

    private DemoProductPhotoCatalog() { }

    /** Returns the demo URL for a known seeded product type; empty means keep the existing fallback. */
    public static List<String> photosForProductName(String productName) {
        if (productName == null || productName.isBlank()) return List.of();
        String normalized = normalize(productName);
        return PHOTO_BY_PRODUCT_TYPE.entrySet().stream()
                .filter(entry -> normalized.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(filename -> "/api/dev-demo/images/parts/" + filename)
                .findFirst()
                .map(List::of)
                .orElseGet(List::of);
    }

    public static Map<String, String> productTypeToFilename() {
        return PHOTO_BY_PRODUCT_TYPE;
    }

    private static String normalize(String value) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return ascii.toLowerCase(Locale.ROOT).replace('ı', 'i').trim();
    }
}
