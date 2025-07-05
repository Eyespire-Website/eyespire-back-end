package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.DoctorDTO;
import org.eyespire.eyespireapi.dto.LoginCredentialsEmailRequest;
import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.eyespire.eyespireapi.service.DoctorService;
import org.eyespire.eyespireapi.service.EmailService;
import org.eyespire.eyespireapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private DoctorService doctorService;

    // Lấy danh sách tất cả nhân viên (không bao gồm bệnh nhân và admin)
    @GetMapping("/staff")
    public ResponseEntity<List<User>> getAllStaff() {
        List<User> staffList = userService.findAllByRoleIn(
                Arrays.asList(UserRole.DOCTOR, UserRole.RECEPTIONIST, UserRole.STORE_MANAGER)
        );
        return ResponseEntity.ok(staffList);
    }

    // Lấy thông tin nhân viên theo ID
    @GetMapping("/staff/{id}")
    public ResponseEntity<User> getStaffById(@PathVariable Integer id) {
        User staff = userService.findById(id);
        if (staff == null || staff.getRole() == UserRole.PATIENT) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(staff);
    }

    // Lấy danh sách nhân viên theo chức vụ
    @GetMapping("/staff/position/{position}")
    public ResponseEntity<List<User>> getStaffByPosition(@PathVariable String position) {
        try {
            UserRole role = UserRole.valueOf(position.toUpperCase());
            if (role == UserRole.PATIENT) {
                return ResponseEntity.badRequest().build();
            }
            List<User> staffList = userService.findAllByRole(role);
            return ResponseEntity.ok(staffList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Thêm nhân viên mới
    @PostMapping("/staff")
    public ResponseEntity<User> createStaff(@RequestBody User user) {
        // Kiểm tra nếu role là PATIENT thì không cho phép
        if (user.getRole() == UserRole.PATIENT) {
            return ResponseEntity.badRequest().build();
        }
        User createdStaff = userService.createUser(user);
        return ResponseEntity.ok(createdStaff);
    }

    // Cập nhật thông tin nhân viên
    @PutMapping("/staff/{id}")
    public ResponseEntity<User> updateStaff(@PathVariable Integer id, @RequestBody User user) {
        User existingStaff = userService.findById(id);
        if (existingStaff == null || existingStaff.getRole() == UserRole.PATIENT) {
            return ResponseEntity.notFound().build();
        }
        
        // Kiểm tra nếu cố gắng đổi role thành PATIENT
        if (user.getRole() == UserRole.PATIENT) {
            return ResponseEntity.badRequest().build();
        }
        
        user.setId(id);
        User updatedStaff = userService.updateUser(user);
        return ResponseEntity.ok(updatedStaff);
    }

    // Xóa nhân viên
    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteStaff(@PathVariable Integer id) {
        User existingStaff = userService.findById(id);
        if (existingStaff == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Nếu là bệnh nhân, không cho phép xóa
        if (existingStaff.getRole() == UserRole.PATIENT) {
            // Khóa tài khoản bệnh nhân thay vì xóa
            userService.updateUserStatus(id, "blocked");
            return ResponseEntity.ok(Map.of("blocked", true));
        }
        
        // Xóa nhân viên khỏi database
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // Gửi thông tin đăng nhập qua email
    @PostMapping("/send-login-email")
    public ResponseEntity<?> sendLoginEmail(@RequestBody LoginCredentialsEmailRequest request) {
        try {
            // Kiểm tra email có tồn tại không
            if (request.getRecipientEmail() == null || request.getRecipientEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email không được để trống"
                ));
            }
            
            // Gửi email thông tin đăng nhập
            emailService.sendLoginCredentialsEmail(
                request.getRecipientEmail(),
                request.getRecipientName(),
                request.getUsername(),
                request.getPassword(),
                request.getSubject()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã gửi thông tin đăng nhập qua email thành công"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Gửi email thất bại: " + e.getMessage()
            ));
        }
    }
    
    // Lấy thông tin bác sĩ theo User ID
    @GetMapping("/doctors/user/{userId}")
    public ResponseEntity<?> getDoctorByUserId(@PathVariable Integer userId) {
        try {
            Optional<Doctor> doctorOpt = doctorService.getDoctorByUserId(userId);
            if (doctorOpt.isPresent()) {
                return ResponseEntity.ok(doctorOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Lỗi khi lấy thông tin bác sĩ: " + e.getMessage()
            ));
        }
    }
    
    // Tạo mới thông tin bác sĩ
    @PostMapping("/doctors")
    public ResponseEntity<?> createDoctor(@RequestBody DoctorDTO doctorDTO) {
        try {
            Doctor createdDoctor = doctorService.createDoctor(doctorDTO);
            return ResponseEntity.ok(createdDoctor);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Lỗi khi tạo thông tin bác sĩ: " + e.getMessage()
            ));
        }
    }
    
    // Cập nhật thông tin bác sĩ
    @PutMapping("/doctors/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable Integer id, @RequestBody DoctorDTO doctorDTO) {
        try {
            Doctor updatedDoctor = doctorService.updateDoctor(id, doctorDTO);
            return ResponseEntity.ok(updatedDoctor);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Lỗi khi cập nhật thông tin bác sĩ: " + e.getMessage()
            ));
        }
    }

    // === USER MANAGEMENT ENDPOINTS ===
    
    // Lấy tất cả người dùng với phân trang và sắp xếp
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            Map<String, Object> response = userService.getAllUsersWithPagination(page, size, sortBy, sortDir);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách người dùng: " + e.getMessage());
        }
    }

    // Lấy người dùng theo ID
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        try {
            User user = userService.getUserById(id);
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy người dùng");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thông tin người dùng: " + e.getMessage());
        }
    }

    // Tìm kiếm người dùng theo từ khóa
    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Map<String, Object> response = userService.searchUsers(keyword, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tìm kiếm người dùng: " + e.getMessage());
        }
    }

    // Lọc người dùng theo vai trò
    @GetMapping("/users/filter/role/{role}")
    public ResponseEntity<?> filterUsersByRole(
            @PathVariable String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            UserRole userRole;
            try {
                userRole = UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Vai trò không hợp lệ");
            }
            
            Map<String, Object> response = userService.getUsersByRole(userRole, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lọc người dùng theo vai trò: " + e.getMessage());
        }
    }

    // Lọc người dùng theo trạng thái (active, inactive, blocked)
    @GetMapping("/users/filter/status/{status}")
    public ResponseEntity<?> filterUsersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            if (!status.equals("active") && !status.equals("inactive") && !status.equals("blocked")) {
                return ResponseEntity.badRequest().body("Trạng thái không hợp lệ");
            }
            
            Map<String, Object> response = userService.getUsersByStatus(status, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lọc người dùng theo trạng thái: " + e.getMessage());
        }
    }

    // Thêm người dùng mới
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi tạo người dùng mới: " + e.getMessage());
        }
    }

    // Cập nhật thông tin người dùng
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody User user) {
        try {
            user.setId(id);
            User updatedUser = userService.updateUser(user);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi cập nhật thông tin người dùng: " + e.getMessage());
        }
    }

    // Khóa/mở khóa người dùng
    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable Integer id,
            @RequestParam String status) {
        try {
            if (!status.equals("active") && !status.equals("inactive") && !status.equals("blocked")) {
                return ResponseEntity.badRequest().body("Trạng thái không hợp lệ");
            }
            
            User updatedUser = userService.updateUserStatus(id, status);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi thay đổi trạng thái người dùng: " + e.getMessage());
        }
    }
}
