package com.parca.yedekparca.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.UUID;

@Service
public class R2ObjectStorageService implements ObjectStorageService {
    private static final Logger log = LoggerFactory.getLogger(R2ObjectStorageService.class);
    private static final Pattern OFFER_IMAGE_KEY = Pattern.compile("offers/[A-Za-z0-9][A-Za-z0-9._-]*\\.(?:jpg|jpeg|png|webp)", Pattern.CASE_INSENSITIVE);

    @Value("${app.storage.r2.endpoint:}")
    private String endpoint;
    @Value("${app.storage.r2.region:auto}")
    private String region;
    @Value("${app.storage.r2.bucket:}")
    private String bucket;
    @Value("${app.storage.r2.access-key-id:}")
    private String accessKey;
    @Value("${app.storage.r2.secret-access-key:}")
    private String secretKey;
    @Value("${app.storage.r2.public-base-url:}")
    private String publicBaseUrl;

    @PostConstruct
    void logConfigurationState() {
        log.info("R2 configuration loaded: endpointSet={}, bucketSet={}, accessKeySet={}, secretKeySet={}, publicBaseUrlSet={}, regionSet={}",
                !endpoint.isBlank(), !bucket.isBlank(), !accessKey.isBlank(), !secretKey.isBlank(),
                !publicBaseUrl.isBlank(), !region.isBlank());
    }

    private S3Client client() {
        if (endpoint.isBlank() || bucket.isBlank() || accessKey.isBlank() || secretKey.isBlank()) {
            throw new IllegalStateException("R2 configuration is incomplete. Check endpoint, bucket and S3 access credentials.");
        }
        URI endpointUri;
        try {
            endpointUri = URI.create(endpoint);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("R2 S3 endpoint URL is invalid.");
        }
        if (!"https".equalsIgnoreCase(endpointUri.getScheme()) || endpointUri.getHost() == null
                || !endpointUri.getHost().endsWith(".r2.cloudflarestorage.com")) {
            throw new IllegalStateException("R2_ENDPOINT must be the HTTPS S3 API endpoint, not the public development URL.");
        }
        if (accessKey.length() != 32) {
            throw new IllegalStateException("R2_ACCESS_KEY_ID must be the 32-character S3 Access Key ID from the R2 S3 API token. Do not use a Cloudflare REST API token or its bearer value.");
        }

        return S3Client.builder()
                .endpointOverride(endpointUri)
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .build();
    }

    @Override
    public String put(String key, String contentType, byte[] bytes) {
        String publicUrl = createPublicUrl(key);

        try (S3Client s3 = client()) {
            PutObjectResponse response = s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));
            log.info("R2 object upload succeeded: statusCode={}, requestId={}, eTagPresent={}",
                    response.sdkHttpResponse().statusCode(), response.responseMetadata().requestId(), response.eTag() != null);
        } catch (S3Exception ex) {
            String code = ex.awsErrorDetails() == null ? "UnknownS3Error" : ex.awsErrorDetails().errorCode();
            String message = safeErrorMessage(ex.awsErrorDetails() == null ? null : ex.awsErrorDetails().errorMessage());
            log.error("R2 object upload rejected by S3 API: statusCode={}, errorCode={}, requestId={}, errorMessage={}",
                    ex.statusCode(), code, ex.requestId(), message);
            throw new IllegalStateException("R2 upload rejected the request (" + code + "): " + message);
        } catch (SdkClientException ex) {
            String message = safeErrorMessage(ex.getMessage());
            log.error("R2 object upload could not reach the S3 API: exceptionType={}, errorMessage={}", ex.getClass().getSimpleName(), message);
            throw new IllegalStateException("R2 S3 API connection failed: " + message);
        }

        return publicUrl;
    }

    @Override
    public String publicUrl(String key, String storedUrl) {
        validateImageKey(key);
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return storedUrl;
        }
        return createPublicUrl(key);
    }

    @Override
    public void delete(String key) {
        validateImageKey(key);
        try (S3Client s3 = client()) {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception ex) {
            String code = ex.awsErrorDetails() == null ? "UnknownS3Error" : ex.awsErrorDetails().errorCode();
            String message = safeErrorMessage(ex.awsErrorDetails() == null ? null : ex.awsErrorDetails().errorMessage());
            log.error("R2 object deletion rejected by S3 API: statusCode={}, errorCode={}, requestId={}, errorMessage={}",
                    ex.statusCode(), code, ex.requestId(), message);
            throw new IllegalStateException("R2 object deletion failed (" + code + "): " + message);
        } catch (SdkClientException ex) {
            String message = safeErrorMessage(ex.getMessage());
            log.error("R2 object deletion could not reach the S3 API: exceptionType={}, errorMessage={}", ex.getClass().getSimpleName(), message);
            throw new IllegalStateException("R2 S3 API connection failed during deletion: " + message);
        }
    }

    @Override
    public String newKey(String extension) {
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!normalizedExtension.matches("jpg|jpeg|png|webp")) {
            throw new IllegalArgumentException("Only JPG, JPEG, PNG and WebP images are supported.");
        }
        return "offers/" + UUID.randomUUID() + "." + normalizedExtension;
    }

    private String createPublicUrl(String key) {
        validateImageKey(key);
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalStateException("R2_PUBLIC_BASE_URL must be set to a public HTTPS image base URL.");
        }

        URI baseUri;
        try {
            baseUri = URI.create(publicBaseUrl.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("R2_PUBLIC_BASE_URL is invalid.");
        }
        String path = baseUri.getRawPath();
        if (!"https".equalsIgnoreCase(baseUri.getScheme()) || baseUri.getHost() == null
                || baseUri.getUserInfo() != null || baseUri.getQuery() != null || baseUri.getFragment() != null
                || (path != null && !path.isEmpty() && !path.matches("/+"))) {
            throw new IllegalStateException("R2_PUBLIC_BASE_URL must be a public HTTPS origin without a path, query, or fragment.");
        }

        // Validated object keys contain only URL-safe ASCII characters and exactly one slash.
        return publicBaseUrl.trim().replaceAll("/+$", "") + "/" + key;
    }

    private void validateImageKey(String key) {
        if (key == null || !OFFER_IMAGE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid offer image object key.");
        }
    }

    private String safeErrorMessage(String message) {
        if (message == null || message.isBlank()) return "No error message returned";
        String sanitized = message;
        if (accessKey != null && !accessKey.isBlank()) sanitized = sanitized.replace(accessKey, "[REDACTED]");
        if (secretKey != null && !secretKey.isBlank()) sanitized = sanitized.replace(secretKey, "[REDACTED]");
        return sanitized.replaceAll("(?i)(authorization|x-amz-credential|x-amz-signature)=?[^\\s,;]+", "$1=[REDACTED]")
                .replaceAll("[\\r\\n\\t]+", " ");
    }
}
