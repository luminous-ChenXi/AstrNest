package com.chenxi.astrnest.storage;

public record StoredObject(String objectKey, String storedFileName, String publicUrl, long size, String absolutePath, String providerKey) {}
