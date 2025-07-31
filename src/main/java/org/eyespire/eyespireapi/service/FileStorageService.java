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
        
        if (azureBlobStorageService == null) {
            throw new RuntimeException("AzureBlobStorageService is not initialized");
        }
        
        try {
            return azureBlobStorageService.storeFiles(files);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload files to Azure Blob Storage: " + e.getMessage(), e);
        }
    }

    /**
     * Lưu trữ một file ảnh duy nhất và trả về URL
     * @param file File ảnh cần lưu trữ
     * @param subDirectory Thư mục con để lưu trữ
     * @return URL của file đã lưu
     */
    public String storeImage(MultipartFile file, String subDirectory) {
        System.out.println("[FileStorageService] storeImage called - using Azure Blob Storage only");
        System.out.println("[FileStorageService] azureBlobStorageService is null: " + (azureBlobStorageService == null));
        
        if (azureBlobStorageService == null) {
            throw new RuntimeException("AzureBlobStorageService is not initialized");
        }
        
        try {
            System.out.println("[FileStorageService] Calling Azure Blob Storage service");
            String result = azureBlobStorageService.storeImage(file, subDirectory);
            System.out.println("[FileStorageService] Azure upload successful, URL: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("[FileStorageService] ERROR: Azure upload failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to upload to Azure Blob Storage: " + e.getMessage(), e);
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
        
        if (azureBlobStorageService == null) {
            throw new RuntimeException("AzureBlobStorageService is not initialized");
        }
        
        try {
            azureBlobStorageService.deleteFile(fileUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from Azure Blob Storage: " + e.getMessage(), e);
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
