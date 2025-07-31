package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@CrossOrigin(origins = {"https://eyespire.vercel.app", "http://localhost:3000"}, allowCredentials = "true")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            // For Azure Blob Storage, redirect to the blob URL
            if ("azure".equalsIgnoreCase(fileStorageService.getStorageType())) {
                // For Azure, the URL is already complete, redirect to it
                return ResponseEntity.status(302)
                        .header("Location", "https://eyespirestorage25.blob.core.windows.net/eyespire-images/" + filename)
                        .build();
            }
            
            // For local storage, serve the file directly
            Path file = Paths.get(System.getProperty("java.io.tmpdir"), "eyespire", "uploads", filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // Adjust based on file type if needed
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}