package org.eyespire.eyespireapi.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Value("${app.storage.type:local}")
    private String storageType;
    
    @Value("${azure.storage.connection-string:}")
    private String azureConnectionString;
    
    @Value("${azure.storage.container-name:eyespire-images}")
    private String containerName;
    
    @Autowired
    private AzureBlobStorageService azureBlobStorageService;

    public FileStorageService(@Value("${app.upload.dir:${user.home}/eyespire/uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory: " + fileStorageLocation, ex);
        }
        System.out.println("[FileStorageService] Initialized with uploadDir: " + uploadDir);
    }

    /**
     * Lưu trữ nhiều file và trả về chuỗi các URL, phân tách bằng dấu chấm phẩy
     * @param files Mảng các file cần lưu trữ
     * @return Chuỗi các URL, phân tách bằng dấu chấm phẩy
     */
    public String storeFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return "";
        }
        
        if ("azure".equalsIgnoreCase(storageType)) {
            return azureBlobStorageService.storeFiles(files);
        } else {
            // Local storage implementation
            try {
                StringBuilder fileUrls = new StringBuilder();
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                        Path targetLocation = fileStorageLocation.resolve(fileName);
                        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                        fileUrls.append("/uploads/").append(fileName).append(";");
                    }
                }
                // Xóa dấu chấm phẩy cuối nếu có
                if (fileUrls.length() > 0) {
                    fileUrls.deleteCharAt(fileUrls.length() - 1);
                }
                return fileUrls.toString();
            } catch (IOException ex) {
                throw new RuntimeException("Could not store files", ex);
            }
        }
    }

    /**
     * Lưu trữ một file ảnh duy nhất và trả về URL
     * @param file File ảnh cần lưu trữ
     * @param subDirectory Thư mục con để lưu trữ
     * @return URL của file đã lưu
     */
    public String storeImage(MultipartFile file, String subDirectory) {
        System.out.println("[FileStorageService] storeImage called with storageType: " + storageType);
        System.out.println("[FileStorageService] azureBlobStorageService is null: " + (azureBlobStorageService == null));
        if ("azure".equalsIgnoreCase(storageType)) {
            System.out.println("[FileStorageService] Using Azure Blob Storage");
            if (azureBlobStorageService == null) {
                System.out.println("[FileStorageService] ERROR: azureBlobStorageService is null, falling back to local storage");
            } else {
                try {
                    return azureBlobStorageService.storeImage(file, subDirectory);
                } catch (Exception e) {
                    System.out.println("[FileStorageService] ERROR: Azure upload failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        System.out.println("[FileStorageService] Using Local Storage");
            // Local storage implementation
            try {
                if (file.isEmpty()) {
                    throw new RuntimeException("Failed to store empty file");
                }

                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String fileName = UUID.randomUUID().toString() + extension;

                Path targetLocation = fileStorageLocation.resolve(fileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                return "/uploads/" + fileName;
            } catch (IOException ex) {
                throw new RuntimeException("Could not store file " + file.getOriginalFilename(), ex);
            }
        }
    }

    /**
     * Xóa file dựa trên URL
     * @param fileUrl URL của file cần xóa
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        
        if ("azure".equalsIgnoreCase(storageType)) {
            azureBlobStorageService.deleteFile(fileUrl);
        } else {
            // Local storage implementation
            try {
                // Loại bỏ tiền tố "/uploads/" để lấy tên file
                String fileName = fileUrl.startsWith("/uploads/") ? fileUrl.substring("/uploads/".length()) : fileUrl;
                Path filePath = fileStorageLocation.resolve(fileName).normalize();
                // Đảm bảo file nằm trong thư mục lưu trữ để tránh xóa file ngoài ý muốn
                if (filePath.startsWith(fileStorageLocation)) {
                    Files.deleteIfExists(filePath);
                } else {
                    throw new SecurityException("Attempt to delete file outside of storage directory: " + filePath);
                }
            } catch (IOException ex) {
                throw new RuntimeException("Could not delete file: " + fileUrl, ex);
            } catch (SecurityException ex) {
                throw new RuntimeException("Security violation while deleting file: " + fileUrl, ex);
            }
        }
    }

    /**
     * Lấy đường dẫn thư mục lưu trữ file
     * @return Đường dẫn thư mục lưu trữ
     */
    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
    
    /**
     * Lấy loại storage đang sử dụng
     * @return Loại storage (local hoặc azure)
     */
    public String getStorageType() {
        return storageType;
    }
}