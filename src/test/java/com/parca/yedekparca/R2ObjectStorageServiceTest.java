package com.parca.yedekparca;

import com.parca.yedekparca.service.R2ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class R2ObjectStorageServiceTest {
    @Test
    void rejectsPublicDevelopmentUrlWhenConfiguredAsS3Endpoint() {
        R2ObjectStorageService storage = configuredStorage("https://pub-example.r2.dev");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> storage.put("offers/smoke.jpg", "image/jpeg", new byte[]{1}));

        assertTrue(error.getMessage().contains("R2_ENDPOINT"));
    }

    @Test
    void reportsMissingS3CredentialsBeforeMakingNetworkRequest() {
        R2ObjectStorageService storage = configuredStorage("https://account.r2.cloudflarestorage.com");
        ReflectionTestUtils.setField(storage, "accessKey", "");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> storage.put("offers/smoke.jpg", "image/jpeg", new byte[]{1}));

        assertTrue(error.getMessage().contains("incomplete"));
    }

    @Test
    void rejectsCloudflareCredentialValuesThatAreNotS3AccessKeyIdsBeforeNetworkRequest() {
        R2ObjectStorageService storage = configuredStorage("https://account.r2.cloudflarestorage.com");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> storage.put("offers/smoke.jpg", "image/jpeg", new byte[]{1}));

        assertTrue(error.getMessage().contains("32-character S3 Access Key ID"));
    }

    @Test
    void rebuildsHistoricalPhotoUrlFromObjectKeyUsingCurrentWorkerBaseUrl() {
        R2ObjectStorageService storage = configuredStorage("https://account.r2.cloudflarestorage.com");
        ReflectionTestUtils.setField(storage, "publicBaseUrl", "https://yedekparca-image-proxy.yedekparca.workers.dev///");

        String url = storage.publicUrl("offers/229a06b3-3e2f-4702-87b8-44085e9ddb05.jpg",
                "https://old-bucket.r2.dev/offers/old-value.jpg");

        assertEquals("https://yedekparca-image-proxy.yedekparca.workers.dev/offers/229a06b3-3e2f-4702-87b8-44085e9ddb05.jpg", url);
    }

    @Test
    void createsWorkerUrlsForEverySupportedImageExtension() {
        R2ObjectStorageService storage = configuredStorage("https://account.r2.cloudflarestorage.com");
        ReflectionTestUtils.setField(storage, "publicBaseUrl", "https://images.example.workers.dev");

        for (String extension : new String[]{"jpg", "jpeg", "png", "webp"}) {
            String key = "offers/550e8400-e29b-41d4-a716-446655440000." + extension;
            assertEquals("https://images.example.workers.dev/" + key, storage.publicUrl(key, null));
        }
    }

    @Test
    void retainsStoredUrlForCompatibilityWhenPublicBaseUrlIsNotConfigured() {
        R2ObjectStorageService storage = configuredStorage("https://account.r2.cloudflarestorage.com");
        ReflectionTestUtils.setField(storage, "publicBaseUrl", "");
        String oldUrl = "https://old-bucket.r2.dev/offers/image.jpg";

        assertEquals(oldUrl, storage.publicUrl("offers/image.jpg", oldUrl));
    }

    @Test
    void rejectsTraversalOrUnsupportedObjectKeysWhenBuildingPublicUrls() {
        R2ObjectStorageService storage = configuredStorage("https://account.r2.cloudflarestorage.com");

        assertThrows(IllegalArgumentException.class, () -> storage.publicUrl("offers/../secret.jpg", null));
        assertThrows(IllegalArgumentException.class, () -> storage.publicUrl("uploads/image.svg", null));
        assertThrows(IllegalArgumentException.class, () -> storage.publicUrl("offers/nested/image.png", null));
    }

    @Test
    void rejectsPublicBaseUrlsWithPathsQueriesOrNonHttpsSchemes() {
        R2ObjectStorageService storage = configuredStorage("https://account.r2.cloudflarestorage.com");

        for (String invalidBase : new String[]{"http://images.example.workers.dev", "https://images.example.workers.dev/path", "https://images.example.workers.dev?x=1"}) {
            ReflectionTestUtils.setField(storage, "publicBaseUrl", invalidBase);
            assertThrows(IllegalStateException.class, () -> storage.publicUrl("offers/image.jpg", null));
        }
    }

    private R2ObjectStorageService configuredStorage(String endpoint) {
        R2ObjectStorageService storage = new R2ObjectStorageService();
        ReflectionTestUtils.setField(storage, "endpoint", endpoint);
        ReflectionTestUtils.setField(storage, "region", "auto");
        ReflectionTestUtils.setField(storage, "bucket", "test-bucket");
        ReflectionTestUtils.setField(storage, "accessKey", "test-access");
        ReflectionTestUtils.setField(storage, "secretKey", "test-secret");
        ReflectionTestUtils.setField(storage, "publicBaseUrl", "https://pub-example.r2.dev");
        return storage;
    }
}
