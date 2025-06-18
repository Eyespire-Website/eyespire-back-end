package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.PayOSCreateRequest;
import org.eyespire.eyespireapi.dto.PayOSCreateResponse;
import org.eyespire.eyespireapi.dto.PayOSVerifyResponse;
import org.eyespire.eyespireapi.model.Payment;
import org.eyespire.eyespireapi.service.PayOSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments/payment/payos")
@CrossOrigin(origins = "http://localhost:3000")
public class PayOSController {

    @Autowired
    private PayOSService payOSService;

    @PostMapping
    public ResponseEntity<?> createPayOSPayment(@RequestBody PayOSCreateRequest request) {
        try {
            PayOSCreateResponse response = payOSService.createPayment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tạo thanh toán PayOS: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> checkPayOSStatus(@PathVariable Integer id) {
        try {
            Payment payment = payOSService.getPaymentStatus(id);
            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", payment.getId());
            response.put("status", payment.getStatus());
            response.put("amount", payment.getAmount());
            response.put("transactionNo", payment.getTransactionNo());
            response.put("paymentDate", payment.getPaymentDate());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi kiểm tra trạng thái thanh toán: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayOSReturn(@RequestBody Map<String, String> params) {
        try {
            PayOSVerifyResponse response = payOSService.verifyPaymentReturn(params);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xác thực kết quả thanh toán: " + e.getMessage());
        }
    }
}
