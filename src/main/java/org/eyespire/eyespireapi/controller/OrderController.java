package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.OrderDTO;
import org.eyespire.eyespireapi.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Tạo đơn hàng từ giỏ hàng
     * @param request Thông tin đơn hàng
     * @return Thông tin đơn hàng đã tạo
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String shippingAddress = request.get("shippingAddress").toString();
            
            OrderDTO orderDTO = orderService.createOrderFromCart(userId, shippingAddress);
            return ResponseEntity.ok(orderDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tạo đơn hàng: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin đơn hàng theo ID
     * @param id ID của đơn hàng
     * @return Thông tin đơn hàng
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Integer id) {
        try {
            OrderDTO orderDTO = orderService.getOrderById(id);
            return ResponseEntity.ok(orderDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thông tin đơn hàng: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách đơn hàng của người dùng
     * @param userId ID của người dùng
     * @return Danh sách đơn hàng
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getOrdersByUserId(@PathVariable Long userId) {
        try {
            List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách đơn hàng: " + e.getMessage());
        }
    }
}
