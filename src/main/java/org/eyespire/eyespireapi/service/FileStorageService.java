package org.eyespire.eyespireapi.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    public FileStorageService() {
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }

    public String storeFiles(MultipartFile[] files) {
        try {
            StringBuilder fileUrls = new StringBuilder();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    Path targetLocation = fileStorageLocation.resolve(fileName);
                    Files.copy(file.getInputStream(), targetLocation);
                    fileUrls.append(targetLocation.toString()).append(";");
                }
            }
            return fileUrls.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file", ex);
        }
    }
}