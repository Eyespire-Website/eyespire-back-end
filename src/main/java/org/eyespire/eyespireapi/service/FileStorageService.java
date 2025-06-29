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

    /**
     * Lưu trữ nhiều file và trả về chuỗi các URL tương đối, phân tách bằng dấu chấm phẩy
     * @param files Mảng các file cần lưu trữ
     * @return Chuỗi các URL tương đối, phân tách bằng dấu chấm phẩy
     */
    public String storeFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return "";
        }
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

    /**
     * Lưu trữ một file ảnh duy nhất và trả về URL tương đối
     * @param file File ảnh cần lưu trữ
     * @param subDirectory Thư mục con để lưu trữ (không còn sử dụng)
     * @return URL tương đối của file đã lưu
     */
    public String storeImage(MultipartFile file, String subDirectory) {
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

    /**
     * Xóa file dựa trên URL tương đối
     * @param fileUrl URL tương đối của file cần xóa (ví dụ: /uploads/filename)
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
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

    /**
     * Lấy đường dẫn thư mục lưu trữ file
     * @return Đường dẫn thư mục lưu trữ
     */
    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
}