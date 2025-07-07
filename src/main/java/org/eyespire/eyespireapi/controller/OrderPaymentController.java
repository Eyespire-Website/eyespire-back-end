package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.OrderPayOSCreateResponse;
import org.eyespire.eyespireapi.dto.PayOSVerifyResponse;
import org.eyespire.eyespireapi.model.Order;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.repository.OrderRepository;
import org.eyespire.eyespireapi.service.OrderPaymentService;
import org.eyespire.eyespireapi.service.OrderService;
import org.eyespire.eyespireapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders/payment/payos")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderPaymentController {

    @Autowired
    private OrderPaymentService orderPaymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    /**
     * Tạo thanh toán cho đơn hàng (endpoint mới để tương thích với frontend)
     * @param requestBody Body của request chứa orderId
     * @return Thông tin thanh toán
     */
    @PostMapping("")
    public ResponseEntity<?> createPaymentFromBody(@RequestBody Map<String, Object> requestBody) {
        try {
            // Log request body để debug
            System.out.println("Request body: " + requestBody);
            
            // Lấy orderId từ request body
            if (!requestBody.containsKey("orderId")) {
                return ResponseEntity.badRequest().body("Thiếu thông tin orderId");
            }
            
            Integer orderId;
            try {
                orderId = Integer.parseInt(requestBody.get("orderId").toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("orderId không hợp lệ");
            }
            
            // Lấy thông tin đơn hàng
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
            
            // Lấy userId từ request body hoặc sử dụng userId từ đơn hàng
            Integer userId = null;
            if (requestBody.containsKey("userId")) {
                try {
                    userId = Integer.parseInt(requestBody.get("userId").toString());
                } catch (NumberFormatException e) {
                    // Nếu không chuyển đổi được, sử dụng userId từ đơn hàng
                    userId = order.getPatient().getId();
                }
            } else {
                // Nếu không có userId trong request, sử dụng userId từ đơn hàng
                userId = order.getPatient().getId();
            }
            
            // Lấy thông tin người dùng bằng getUserById thay vì getCurrentUser
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy thông tin người dùng");
            }

            // Lấy returnUrl từ request body nếu có
            String returnUrl = requestBody.containsKey("returnUrl") ? 
                requestBody.get("returnUrl").toString() : null;
                
            // Tạo thanh toán
            OrderPayOSCreateResponse response = orderPaymentService.createOrderPayment(order, user, returnUrl);
            
            // Log response để debug
            System.out.println("Payment response: " + response);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log lỗi chi tiết
            System.err.println("Lỗi khi tạo thanh toán: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tạo thanh toán: " + e.getMessage());
        }
    }

    /**
     * Tạo thanh toán cho đơn hàng
     * @param orderId ID của đơn hàng cần thanh toán
     * @return Thông tin thanh toán
     */
    @PostMapping("/create/{orderId}")
    public ResponseEntity<?> createPayment(@PathVariable Integer orderId) {
        try {
            // Lấy thông tin đơn hàng
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
            if (order == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy đơn hàng");
            }

            // Lấy thông tin người dùng từ đơn hàng
            Integer userId = order.getPatient().getId();
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy thông tin người dùng");
            }

            // Tạo thanh toán (sử dụng null cho returnUrl để sử dụng URL mặc định)
            OrderPayOSCreateResponse response = orderPaymentService.createOrderPayment(order, user, null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tạo thanh toán: " + e.getMessage());
        }
    }

    /**
     * Xác thực kết quả thanh toán từ PayOS
     * @param params Các tham số từ PayOS callback
     * @return Kết quả xác thực
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> params) {
        try {
            PayOSVerifyResponse response = orderPaymentService.verifyOrderPayment(params);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xác thực thanh toán: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán
     * @param orderId ID của đơn hàng
     * @return Thông tin trạng thái thanh toán
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable Integer orderId) {
        try {
            Map<String, Object> status = orderPaymentService.checkOrderPaymentStatus(orderId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi kiểm tra trạng thái thanh toán: " + e.getMessage());
        }
    }
}
