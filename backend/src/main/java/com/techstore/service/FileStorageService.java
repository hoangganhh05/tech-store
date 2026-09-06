package com.techstore.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeProductImage(MultipartFile file);

    void deleteFile(String fileUrl);
}
