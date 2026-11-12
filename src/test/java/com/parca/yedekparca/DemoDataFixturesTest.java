package com.parca.yedekparca;

import com.parca.yedekparca.controller.DemoImageController;
import com.parca.yedekparca.controller.DemoProductPhotoCatalog;
import com.parca.yedekparca.config.DemoOfferDistribution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemoDataFixturesTest {
    @Test void deterministicOfferDistributionHasExactlyNineHundredOffers(){
        int[] counts=DemoOfferDistribution.counts();
        assertEquals(300,counts.length);
        assertEquals(900,DemoOfferDistribution.total());
        assertTrue(java.util.Arrays.stream(counts).allMatch(n->n==1||n==2||n==3||n==4||n==5||n==8));
        assertTrue(java.util.Arrays.stream(counts).anyMatch(n->n==8));
    }
    @Test void demoImageEndpointHasFiniteAllowlist(){
        assertEquals(13,DemoImageController.PUBLIC_GET_PATHS.size());
        assertTrue(DemoImageController.PUBLIC_GET_PATHS.keySet().containsAll(java.util.Set.of(
                "/api/dev-demo/images/brake.svg", "/api/dev-demo/images/filter.svg", "/api/dev-demo/images/engine.svg",
                "/api/dev-demo/images/parts/brake-pads.jpg", "/api/dev-demo/images/parts/oil-filter.jpg",
                "/api/dev-demo/images/parts/air-filter.jpg", "/api/dev-demo/images/parts/clutch-disc.jpg",
                "/api/dev-demo/images/parts/shock-absorber.jpg", "/api/dev-demo/images/parts/headlamp.jpg",
                "/api/dev-demo/images/parts/water-pump.jpg", "/api/dev-demo/images/parts/ignition-coil.jpg",
                "/api/dev-demo/images/parts/radiator.jpg", "/api/dev-demo/images/parts/control-arm.jpg")));
        assertFalse(DemoImageController.isAllowedPublicGetPath("/api/dev-demo/images/../../application.properties"));
    }

    @Test void productTypesMapToTheirOwnDemoPhotoAndUnknownTypesKeepFallback() {
        assertEquals(java.util.List.of("/api/dev-demo/images/parts/shock-absorber.jpg"),
                DemoProductPhotoCatalog.photosForProductName("Amortisör Test Marka Seri 001"));
        assertEquals(java.util.List.of("/api/dev-demo/images/parts/water-pump.jpg"),
                DemoProductPhotoCatalog.photosForProductName("Su pompası Örnek Seri"));
        assertTrue(DemoProductPhotoCatalog.photosForProductName("Bilinmeyen parça").isEmpty());
        assertEquals(10, DemoProductPhotoCatalog.productTypeToFilename().size());
    }
}
