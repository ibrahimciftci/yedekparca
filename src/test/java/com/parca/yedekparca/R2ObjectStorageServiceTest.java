package com.parca.yedekparca;

import com.parca.yedekparca.service.R2ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
