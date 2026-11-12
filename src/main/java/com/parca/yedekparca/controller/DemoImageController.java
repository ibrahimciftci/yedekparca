package com.parca.yedekparca.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@RestController
@Profile("dev-demo & !prod & !production")
public class DemoImageController {
    private static final Map<String, ImageAsset> IMAGES = Map.ofEntries(
            Map.entry("brake.svg", new ImageAsset("demo-images/brake.svg", MediaType.parseMediaType("image/svg+xml"))),
            Map.entry("filter.svg", new ImageAsset("demo-images/filter.svg", MediaType.parseMediaType("image/svg+xml"))),
            Map.entry("engine.svg", new ImageAsset("demo-images/engine.svg", MediaType.parseMediaType("image/svg+xml"))),
            Map.entry("parts/brake-pads.jpg", jpeg("brake-pads.jpg")),
            Map.entry("parts/oil-filter.jpg", jpeg("oil-filter.jpg")),
            Map.entry("parts/air-filter.jpg", jpeg("air-filter.jpg")),
            Map.entry("parts/clutch-disc.jpg", jpeg("clutch-disc.jpg")),
            Map.entry("parts/shock-absorber.jpg", jpeg("shock-absorber.jpg")),
            Map.entry("parts/headlamp.jpg", jpeg("headlamp.jpg")),
            Map.entry("parts/water-pump.jpg", jpeg("water-pump.jpg")),
            Map.entry("parts/ignition-coil.jpg", jpeg("ignition-coil.jpg")),
            Map.entry("parts/radiator.jpg", jpeg("radiator.jpg")),
            Map.entry("parts/control-arm.jpg", jpeg("control-arm.jpg"))
    );

    public static final Map<String, String> PUBLIC_GET_PATHS = IMAGES.keySet().stream()
            .map(key -> "/api/dev-demo/images/" + key)
            .collect(java.util.stream.Collectors.toUnmodifiableMap(path -> path, path -> path));

    @GetMapping({"/api/dev-demo/images/{filename}", "/api/dev-demo/images/parts/{filename}"})
    public ResponseEntity<byte[]> image(@PathVariable String filename, jakarta.servlet.http.HttpServletRequest request) throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String key = path.substring("/api/dev-demo/images/".length());
        ImageAsset image = IMAGES.get(key);
        if (image == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(image.contentType())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(new ClassPathResource(image.resourcePath()).getContentAsByteArray());
    }

    public static boolean isAllowedPublicGetPath(String path) { return PUBLIC_GET_PATHS.containsKey(path); }

    public static Map<String, String> imagePathsToResources() {
        return IMAGES.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                entry -> "/api/dev-demo/images/" + entry.getKey(), entry -> entry.getValue().resourcePath()));
    }

    private static ImageAsset jpeg(String filename) {
        return new ImageAsset("demo-images/parts/" + filename, MediaType.IMAGE_JPEG);
    }

    private record ImageAsset(String resourcePath, MediaType contentType) { }
}
