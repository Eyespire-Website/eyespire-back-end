package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.OrderDTO;
import org.eyespire.eyespireapi.model.enums.OrderStatus;
import org.eyespire.eyespireapi.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body("Lỗi: " + e.getReason());
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
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body("Lỗi khi lấy thông tin đơn hàng: " + e.getReason());
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
            return ResponseEntity.ok(orders != null ? orders : List.of());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách đơn hàng: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả đơn hàng
     * @return Danh sách tất cả đơn hàng
     */
    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        try {
            List<OrderDTO> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders != null ? orders : List.of());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách tất cả đơn hàng: " + e.getMessage());
        }
    }

    /**
     * Cập nhật đơn hàng
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable Integer id, @RequestBody OrderDTO orderDTO) {
        try {
            orderDTO.setId(id);
            OrderDTO updatedOrder = orderService.updateOrder(orderDTO);
            return ResponseEntity.ok(updatedOrder);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body("Lỗi khi cập nhật đơn hàng: " + e.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi cập nhật đơn hàng: " + e.getMessage());
        }
    }

    /**
     * Cập nhật trạng thái đơn hàng
     */
    // CHANGE: Enhanced updateOrderStatus with logging and better error handling
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Integer id, @RequestBody String status) {
        try {
            log.info("Received request to update order {} to status {}", id, status);
            if (status == null || status.trim().isEmpty()) {
                log.warn("Status is empty for order ID: {}", id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Trạng thái không được để trống");
            }
            try {
                OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid status: {}. Valid statuses: {}", status, Arrays.toString(OrderStatus.values()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Trạng thái không hợp lệ: " + status + ". Các giá trị hợp lệ: " + Arrays.toString(OrderStatus.values()));
            }
            OrderDTO updatedOrder = orderService.updateOrderStatus(id, status.toUpperCase());
            log.info("Successfully updated order {} to status {}", id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (ResponseStatusException e) {
            log.error("ResponseStatusException in updateOrderStatus: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                    .body("Lỗi khi cập nhật trạng thái đơn hàng: " + e.getReason());
        } catch (Exception e) {
            log.error("Unexpected error in updateOrderStatus: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage());
        }
    }
    // END CHANGE

    /**
     * Xóa đơn hàng
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable Integer id) {
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body("Lỗi khi xóa đơn hàng: " + e.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi xóa đơn hàng: " + e.getMessage());
        }
    }
}