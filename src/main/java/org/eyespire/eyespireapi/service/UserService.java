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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    // Phương thức mới để lấy tất cả nhân viên theo danh sách vai trò
    public List<User> findAllByRoleIn(List<UserRole> roles) {
        return userRepository.findByRoleIn(roles);
    }
    
    // Phương thức mới để lấy tất cả nhân viên theo vai trò
    public List<User> findAllByRole(UserRole role) {
        return userRepository.findByRole(role);
    }
    
    // Phương thức lấy tất cả người dùng với phân trang và sắp xếp
    public Map<String, Object> getAllUsersWithPagination(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<User> userPage = userRepository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent());
        response.put("currentPage", userPage.getNumber());
        response.put("totalItems", userPage.getTotalElements());
        response.put("totalPages", userPage.getTotalPages());
        
        return response;
    }
    
    // Phương thức tìm kiếm người dùng theo từ khóa
    public Map<String, Object> searchUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        Page<User> userPage = userRepository.findByNameContainingOrEmailContainingOrPhoneContaining(
                keyword, keyword, keyword, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent());
        response.put("currentPage", userPage.getNumber());
        response.put("totalItems", userPage.getTotalElements());
        response.put("totalPages", userPage.getTotalPages());
        
        return response;
    }
    
    // Phương thức lọc người dùng theo vai trò
    public Map<String, Object> getUsersByRole(UserRole role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        Page<User> userPage = userRepository.findByRole(role, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent());
        response.put("currentPage", userPage.getNumber());
        response.put("totalItems", userPage.getTotalElements());
        response.put("totalPages", userPage.getTotalPages());
        
        return response;
    }
    
    // Phương thức lọc người dùng theo trạng thái
    public Map<String, Object> getUsersByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        Page<User> userPage = userRepository.findByStatus(status, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent());
        response.put("currentPage", userPage.getNumber());
        response.put("totalItems", userPage.getTotalElements());
        response.put("totalPages", userPage.getTotalPages());
        
        return response;
    }
    
    // Phương thức cập nhật trạng thái người dùng (khóa/mở khóa)
    public User updateUserStatus(Integer id, String status) {
        User user = getUserById(id);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }
        
        user.setStatus(status);
        return userRepository.save(user);
    }
    
    // Phương thức mới để tạo người dùng mới (nhân viên)
    public User createUser(User user) {
        // Mã hóa mật khẩu trước khi lưu
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        // Đặt trạng thái mặc định là active nếu chưa được đặt
        if (user.getStatus() == null) {
            user.setStatus("active");
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
        
        // Cập nhật trạng thái nếu có
        if (user.getStatus() != null) {
            existingUser.setStatus(user.getStatus());
        }
        
        // Cập nhật mật khẩu nếu có
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        return userRepository.save(existingUser);
    }
    
    public User updateProfile(Integer id, UpdateProfileRequest request) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            if (request.getName() != null) {
                user.setName(request.getName());
            }
            
            if (request.getPhone() != null) {
                user.setPhone(request.getPhone());
            }
            
            if (request.getGender() != null) {
                try {
                    GenderType gender = GenderType.valueOf(request.getGender().toUpperCase());
                    user.setGender(gender);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Giới tính không hợp lệ");
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
            return "https://eyespire-back-end.onrender.com" + avatarUrl;
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
    
    // Phương thức cập nhật thời gian đăng nhập cuối cùng
    public void updateLastLogin(Integer userId) {
        User user = getUserById(userId);
        if (user != null) {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        }
    }
    
    // Phương thức xóa người dùng (chỉ áp dụng cho nhân viên, không áp dụng cho bệnh nhân)
    public void deleteUser(Integer id) {
        User user = findById(id);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }
        
        // Kiểm tra xem có phải là bệnh nhân không
        if (user.getRole() == UserRole.PATIENT) {
            throw new RuntimeException("Không thể xóa tài khoản bệnh nhân, chỉ có thể khóa");
        }
        
        // Nếu là bác sĩ, xóa thông tin bác sĩ trước
        if (user.getRole() == UserRole.DOCTOR) {
            Optional<Doctor> doctorOptional = doctorRepository.findByUserId(id);
            doctorOptional.ifPresent(doctor -> doctorRepository.delete(doctor));
        }
        
        // Xóa người dùng
        userRepository.deleteById(id);
    }
    
    /**
     * Lấy thông tin người dùng hiện tại từ SecurityContextHolder
     * @return User đang đăng nhập
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
