package org.eyespire.eyespireapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${app.upload.dir:${user.home}/eyespire/uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory: " + fileStorageLocation, ex);
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
    
    /**
     * Lưu trữ một file ảnh duy nhất và trả về URL tương đối
     * @param file File ảnh cần lưu trữ
     * @param subDirectory Thư mục con để lưu trữ (ví dụ: "services", "doctors") - không còn sử dụng
     * @return URL tương đối của file đã lưu
     */
    public String storeImage(MultipartFile file, String subDirectory) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file");
            }
            
            // Tạo tên file duy nhất
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;
            
            // Đường dẫn đầy đủ để lưu file - lưu trực tiếp vào thư mục gốc
            Path targetLocation = fileStorageLocation.resolve(fileName);
            
            // Lưu file
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Trả về URL tương đối - không sử dụng thư mục con
            return "/uploads/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename(), ex);
        }
    }
}