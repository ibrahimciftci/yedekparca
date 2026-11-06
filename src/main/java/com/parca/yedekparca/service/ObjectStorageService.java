package com.parca.yedekparca.service;

public interface ObjectStorageService {
    String newKey(String extension);

    String put(String key, String contentType, byte[] bytes);

    /** Resolves a public image URL; implementations may retain a stored URL as a compatibility fallback. */
    default String publicUrl(String key, String storedUrl) {
        return storedUrl;
    }

    void delete(String key);
}
