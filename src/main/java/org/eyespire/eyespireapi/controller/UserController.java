package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.ChangePasswordRequest;
import org.eyespire.eyespireapi.dto.UpdateProfileRequest;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.eyespire.eyespireapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"https://eyespire.vercel.app", "https://eyespire.vercel.app"}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class UserController {

    @Autowired
    private UserService userService;

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Integer id, @RequestBody UpdateProfileRequest request) {
        try {
            System.out.println("Received update request: " + request);
            User updatedUser = userService.updateProfile(id, request);
            // Trả về toàn bộ thông tin người dùng
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Không thể cập nhật thông tin: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable Integer id, @RequestBody ChangePasswordRequest request) {
        try {
            boolean success = userService.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
            if (success) {
                // Trả về thông tin người dùng sau khi đổi mật khẩu
                User user = userService.getUserById(id);
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Mật khẩu hiện tại không đúng");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Không thể đổi mật khẩu: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateAvatar(@PathVariable Integer id, @RequestParam("avatar") MultipartFile file) {
        try {
            String avatarUrl = userService.updateAvatar(id, file);
            // Trả về URL của avatar
            return ResponseEntity.ok().body(avatarUrl);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Không thể cập nhật ảnh đại diện: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
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
    @GetMapping("/patients")
    public ResponseEntity<?> getAllPatients() {
        try {
            List<User> patients = userService.getAllPatients();
            return ResponseEntity.ok(patients);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách bệnh nhân: " + e.getMessage());
        }
    }
    @PostMapping("/patients")
    public ResponseEntity<?> createPatient(@RequestBody User user) {
        try {
            if (userService.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Email đã tồn tại");
            }
            user.setRole(UserRole.PATIENT);
            user.setStatus("active");
            user.setPassword(null); // Không yêu cầu mật khẩu cho khách hàng
            User newUser = userService.createUser(user);
            return ResponseEntity.ok(newUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Không thể tạo khách hàng: " + e.getMessage());
        }
    }
}
