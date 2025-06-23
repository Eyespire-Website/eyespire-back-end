package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.eyespire.eyespireapi.service.UserService;
import org.eyespire.eyespireapi.service.EmailService;
import org.eyespire.eyespireapi.dto.LoginCredentialsEmailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;

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
        if (existingStaff == null || existingStaff.getRole() == UserRole.PATIENT) {
            return ResponseEntity.notFound().build();
        }
        
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
}
