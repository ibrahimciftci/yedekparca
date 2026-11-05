package com.parca.yedekparca.service;

public interface ObjectStorageService {
    String newKey(String extension);

    String put(String key, String contentType, byte[] bytes);

    void delete(String key);
}
