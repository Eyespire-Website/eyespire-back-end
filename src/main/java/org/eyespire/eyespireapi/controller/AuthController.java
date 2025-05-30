package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.*;
import org.eyespire.eyespireapi.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
                    .body(Map.of("message", "Tên đăng nhập hoặc mật khẩu không đúng"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signupStep1(@RequestBody SignupRequest signupRequest) {
        try {
            authService.signupStep1SendOtp(signupRequest);
            return ResponseEntity.ok("Đã gửi mã OTP tới email.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Xác thực OTP
    @PostMapping("/signup/verify-otp")
    public ResponseEntity<?> signupVerifyOtp(@RequestBody VerifyOtpRequest verifyOtpRequest) {
        LoginResponse response = authService.verifyOtpAndCreateUser(verifyOtpRequest.getEmail(), verifyOtpRequest.getOtp());
        if(response == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP không hợp lệ hoặc đã hết hạn.");
        }
        return ResponseEntity.ok(response);
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
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request.getEmail());
            return ResponseEntity.ok("Đã gửi email hướng dẫn đặt lại mật khẩu");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xử lý yêu cầu đặt lại mật khẩu: " + e.getMessage());
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            boolean success = authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
            if (success) {
                return ResponseEntity.ok("Đặt lại mật khẩu thành công");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể đặt lại mật khẩu");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi đặt lại mật khẩu: " + e.getMessage());
        }
    }
}
