package com.parca.yedekparca.service;

public interface ObjectStorageService {
    String newKey(String extension);

    default String newSellerLogoKey(String extension) { return newKey(extension); }

    String put(String key, String contentType, byte[] bytes);

    /** Resolves a public image URL; implementations may retain a stored URL as a compatibility fallback. */
    default String publicUrl(String key, String storedUrl) {
        return storedUrl;
    }

    void delete(String key);
}
