package com.parca.yedekparca;

import com.parca.yedekparca.service.R2ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Opt-in smoke test. It writes one temporary object, fetches it through the configured public URL, then deletes it. */
class R2LiveUploadTest {
    @Test
    void uploadsReadsAndDeletesOneImageFromConfiguredR2Bucket() throws Exception {
        assumeTrue("true".equalsIgnoreCase(System.getenv("R2_LIVE_TEST")), "Set R2_LIVE_TEST=true to run the live R2 smoke test");

        R2ObjectStorageService storage = new R2ObjectStorageService();
        set(storage, "endpoint", System.getenv("R2_ENDPOINT"));
        set(storage, "region", valueOrDefault("R2_REGION", "auto"));
        set(storage, "bucket", System.getenv("R2_BUCKET"));
        set(storage, "accessKey", System.getenv("R2_ACCESS_KEY_ID"));
        set(storage, "secretKey", System.getenv("R2_SECRET_ACCESS_KEY"));
        set(storage, "publicBaseUrl", System.getenv("R2_PUBLIC_BASE_URL"));

        byte[] image = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/aZkAAAAASUVORK5CYII=");
        String key = storage.newKey("png");
        String publicUrl = null;
        try {
            publicUrl = storage.put(key, "image/png", image);
            HttpResponse<byte[]> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                    .send(HttpRequest.newBuilder(URI.create(publicUrl)).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode(), "Uploaded image must be readable through R2_PUBLIC_BASE_URL");
            assertArrayEquals(image, response.body(), "Public URL must return the uploaded image bytes");
        } finally {
            if (publicUrl != null) storage.delete(key);
        }
    }

    private static void set(R2ObjectStorageService target, String field, String value) {
        ReflectionTestUtils.setField(target, field, value == null ? "" : value);
    }

    private static String valueOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
