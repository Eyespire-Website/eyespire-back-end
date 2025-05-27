package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.GoogleCallbackRequest;
import org.eyespire.eyespireapi.dto.GoogleLoginUrlResponse;
import org.eyespire.eyespireapi.dto.LoginRequest;
import org.eyespire.eyespireapi.dto.LoginResponse;
import org.eyespire.eyespireapi.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Tên đăng nhập hoặc mật khẩu không đúng");
        }
    }
    
    @GetMapping("/google-login-url")
    public ResponseEntity<GoogleLoginUrlResponse> getGoogleLoginUrl() {
        String googleAuthUrl = authService.createGoogleAuthorizationUrl();
        GoogleLoginUrlResponse response = new GoogleLoginUrlResponse(googleAuthUrl);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/google-callback")
    public ResponseEntity<?> handleGoogleCallback(@RequestBody GoogleCallbackRequest callbackRequest) {
        try {
            LoginResponse response = authService.authenticateWithGoogle(callbackRequest.getCode());
            
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Xác thực Google thất bại");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi xử lý xác thực Google: " + e.getMessage());
        }
    }
}
