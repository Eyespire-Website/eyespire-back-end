package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.UpdateProfileRequest;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.GenderType;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Value("${app.upload.dir:${user.home}/eyespire/uploads}")
    private String uploadDir;

    public User getUserById(Integer id) {
        Optional<User> userOptional = userRepository.findById(id);
        return userOptional.orElse(null);
    }

    public User updateProfile(Integer id, UpdateProfileRequest request) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // Cập nhật thông tin người dùng
            if (request.getName() != null) {
                user.setName(request.getName());
            }
            
            if (request.getPhone() != null) {
                user.setPhone(request.getPhone());
            }
            
            if (request.getGender() != null) {
                try {
                    // Chuyển đổi String thành GenderType
                    GenderType genderType = GenderType.valueOf(request.getGender().toUpperCase());
                    user.setGender(genderType);
                } catch (IllegalArgumentException e) {
                    // Nếu không chuyển đổi được, giữ nguyên giá trị cũ
                    System.out.println("Không thể chuyển đổi gender: " + request.getGender());
                }
            }
            
            if (request.getUsername() != null) {
                // Kiểm tra xem username đã tồn tại chưa
                if (!user.getUsername().equals(request.getUsername()) && 
                    userRepository.findByUsername(request.getUsername()).isPresent()) {
                    throw new RuntimeException("Tên đăng nhập đã tồn tại");
                }
                user.setUsername(request.getUsername());
            }
            
            if (request.getBirthdate() != null) {
                user.setDateOfBirth(request.getBirthdate());
            }
            
            if (request.getProvinceCode() != null) {
                user.setProvince(request.getProvinceCode());
            }
            
            if (request.getDistrictCode() != null) {
                user.setDistrict(request.getDistrictCode());
            }
            
            if (request.getWardCode() != null) {
                user.setWard(request.getWardCode());
            }
            
            if (request.getAddress() != null) {
                user.setAddressDetail(request.getAddress());
            }
            
            return userRepository.save(user);
        } else {
            throw new RuntimeException("Không tìm thấy người dùng");
        }
    }

    public boolean changePassword(Integer id, String currentPassword, String newPassword) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // Kiểm tra mật khẩu hiện tại
            if (user.getPassword().equals(currentPassword)) {
                user.setPassword(newPassword);
                userRepository.save(user);
                return true;
            } else {
                return false;
            }
        } else {
            throw new RuntimeException("Không tìm thấy người dùng");
        }
    }

    public String updateAvatar(Integer id, MultipartFile file) throws IOException {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // Tạo thư mục uploads nếu chưa tồn tại
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Tạo tên file duy nhất
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            // Lưu file vào thư mục uploads
            Path filePath = Paths.get(uploadDir, fileName);
            Files.write(filePath, file.getBytes());
            
            // Cập nhật đường dẫn avatar trong database
            String avatarUrl = "/uploads/" + fileName;
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            
            // Trả về đường dẫn đầy đủ của avatar
            return "http://localhost:8080" + avatarUrl;
        } else {
            throw new RuntimeException("Không tìm thấy người dùng");
        }
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return fileName.substring(lastIndexOf);
    }
}
