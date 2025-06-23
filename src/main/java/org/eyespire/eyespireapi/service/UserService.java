package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.UpdateProfileRequest;
import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.GenderType;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.eyespire.eyespireapi.repository.DoctorRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Value("${app.upload.dir:${user.home}/eyespire/uploads}")
    private String uploadDir;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DoctorRepository doctorRepository;

    public User getUserById(Integer id) {
        Optional<User> userOptional = userRepository.findById(id);
        return userOptional.orElse(null);
    }
    
    // Phương thức mới để AdminController sử dụng
    public User findById(Integer id) {
        return getUserById(id);
    }
    
    // Phương thức mới để lấy tất cả nhân viên theo danh sách vai trò
    public List<User> findAllByRoleIn(List<UserRole> roles) {
        return userRepository.findByRoleIn(roles);
    }
    
    // Phương thức mới để lấy tất cả nhân viên theo vai trò
    public List<User> findAllByRole(UserRole role) {
        return userRepository.findByRole(role);
    }
    
    // Phương thức mới để tạo người dùng mới (nhân viên)
    public User createUser(User user) {
        // Mã hóa mật khẩu trước khi lưu
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }
    
    // Phương thức mới để cập nhật người dùng (nhân viên)
    public User updateUser(User user) {
        User existingUser = findById(user.getId());
        if (existingUser == null) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }
        
        // Cập nhật thông tin cơ bản
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setRole(user.getRole());
        
        // Cập nhật các thông tin khác nếu có
        if (user.getGender() != null) {
            existingUser.setGender(user.getGender());
        }
        
        if (user.getDateOfBirth() != null) {
            existingUser.setDateOfBirth(user.getDateOfBirth());
        }
        
        if (user.getProvince() != null) {
            existingUser.setProvince(user.getProvince());
        }
        
        if (user.getDistrict() != null) {
            existingUser.setDistrict(user.getDistrict());
        }
        
        if (user.getWard() != null) {
            existingUser.setWard(user.getWard());
        }
        
        if (user.getAddressDetail() != null) {
            existingUser.setAddressDetail(user.getAddressDetail());
        }
        
        // Cập nhật mật khẩu nếu có
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        return userRepository.save(existingUser);
    }
    
    // Phương thức mới để xóa người dùng (nhân viên)
    public void deleteUser(Integer id) {
        // Kiểm tra xem user có phải là bác sĩ không
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // Nếu là bác sĩ, cần xóa thông tin bác sĩ trước
            if (user.getRole() == UserRole.DOCTOR) {
                Optional<Doctor> doctorOptional = doctorRepository.findByUserId(id);
                if (doctorOptional.isPresent()) {
                    // Xóa bác sĩ trước
                    doctorRepository.delete(doctorOptional.get());
                }
            }
        }
        
        // Sau đó mới xóa user
        userRepository.deleteById(id);
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
            
            // Kiểm tra mật khẩu hiện tại bằng passwordEncoder
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                // Mã hóa mật khẩu mới trước khi lưu
                user.setPassword(passwordEncoder.encode(newPassword));
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

    public List<User> getAllPatients() {
        return userRepository.findByRoleOrderByNameAsc(UserRole.PATIENT);
    }
}
