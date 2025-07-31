package org.eyespire.eyespireapi.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class AzureBlobStorageService {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.container-name:eyespire-images}")
    private String containerName;

    // Account name từ connection string
    private static final String ACCOUNT_NAME = "eyespirestorage25";

    private BlobServiceClient getBlobServiceClient() {
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    /**
     * Upload file lên Azure Blob Storage
     * @param file File cần upload
     * @param subDirectory Thư mục con (có thể null)
     * @return URL đầy đủ của file trên Azure Blob Storage
     */
    public String storeImage(MultipartFile file, String subDirectory) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file");
            }

            // Tạo tên file unique
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            // Thêm thư mục con nếu có
            String blobName = subDirectory != null && !subDirectory.isEmpty() 
                ? subDirectory + "/" + fileName 
                : fileName;

            // Upload lên Azure Blob Storage
            BlobServiceClient blobServiceClient = getBlobServiceClient();
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            
            // Tạo container nếu chưa tồn tại
            if (!containerClient.exists()) {
                containerClient.create();
            }

            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            // Trả về URL đầy đủ
            return blobClient.getBlobUrl();

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename(), ex);
        }
    }

    /**
     * Upload nhiều file lên Azure Blob Storage
     * @param files Mảng các file cần upload
     * @return Chuỗi các URL đầy đủ, phân tách bằng dấu chấm phẩy
     */
    public String storeFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return "";
        }

        StringBuilder fileUrls = new StringBuilder();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileUrl = storeImage(file, null);
                fileUrls.append(fileUrl).append(";");
            }
        }

        // Xóa dấu chấm phẩy cuối nếu có
        if (fileUrls.length() > 0) {
            fileUrls.deleteCharAt(fileUrls.length() - 1);
        }
        return fileUrls.toString();
    }

    /**
     * Xóa file từ Azure Blob Storage
     * @param fileUrl URL đầy đủ của file cần xóa
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // Extract blob name từ URL
            String blobName = extractBlobNameFromUrl(fileUrl);
            if (blobName != null) {
                BlobServiceClient blobServiceClient = getBlobServiceClient();
                BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
                BlobClient blobClient = containerClient.getBlobClient(blobName);
                blobClient.deleteIfExists();
            }
        } catch (Exception ex) {
            System.err.println("Could not delete file: " + fileUrl + ", Error: " + ex.getMessage());
        }
    }

    private String extractBlobNameFromUrl(String fileUrl) {
        // Extract blob name từ Azure Blob Storage URL
        // Format: https://<account>.blob.core.windows.net/<container>/<blobname>
        try {
            String[] parts = fileUrl.split("/" + containerName + "/");
            return parts.length > 1 ? parts[1] : null;
        } catch (Exception e) {
            return null;
        }
    }
}
