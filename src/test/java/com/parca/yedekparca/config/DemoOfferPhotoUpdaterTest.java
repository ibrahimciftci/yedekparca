package com.parca.yedekparca.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DemoOfferPhotoUpdaterTest {
    @Test void acceptsOnlyTheDemoDatabaseUrl() {
        assertDoesNotThrow(() -> DemoOfferPhotoUpdater.validateDemoJdbcUrl(
                "jdbc:postgresql://localhost:5432/yedekparca_demo_db"));
        assertDoesNotThrow(() -> DemoOfferPhotoUpdater.validateDemoJdbcUrl(
                "jdbc:postgresql://localhost:5432/yedekparca_demo_db?currentSchema=public"));
    }

    @Test void refusesNormalDatabaseAndOtherJdbcDrivers() {
        assertThrows(IllegalStateException.class, () -> DemoOfferPhotoUpdater.validateDemoJdbcUrl(
                "jdbc:postgresql://localhost:5432/yedekparca_db"));
        assertThrows(IllegalStateException.class, () -> DemoOfferPhotoUpdater.validateDemoJdbcUrl(
                "jdbc:h2:mem:yedekparca_demo_db"));
    }
}
