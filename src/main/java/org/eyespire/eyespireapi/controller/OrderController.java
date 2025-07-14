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

            // Get shipping fee from request, default to 0 if not provided
            Double shippingFee = 0.0;
            if (request.containsKey("shippingFee") && request.get("shippingFee") != null) {
                shippingFee = Double.valueOf(request.get("shippingFee").toString());
            }

            OrderDTO orderDTO = orderService.createOrderFromCart(userId, shippingAddress, shippingFee);
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
     * Tạo đơn hàng tại quầy
     */
    @PostMapping("/instore")
    public ResponseEntity<?> createInStoreOrder(@RequestBody Map<String, Object> request) {
        try {
            log.info("Received in-store order request: {}", request);

            if (!request.containsKey("userId")) {
                throw new IllegalArgumentException("userId là bắt buộc");
            }
            Long userId;
            try {
                userId = Long.valueOf(request.get("userId").toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("userId không hợp lệ");
            }

            if (!request.containsKey("items") || !(request.get("items") instanceof List)) {
                throw new IllegalArgumentException("Danh sách sản phẩm (items) là bắt buộc");
            }
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
            if (items.isEmpty()) {
                throw new IllegalArgumentException("Phải có ít nhất một sản phẩm trong đơn hàng");
            }

            if (!request.containsKey("paymentMethod")) {
                throw new IllegalArgumentException("Phương thức thanh toán là bắt buộc");
            }
            String paymentMethod = request.get("paymentMethod").toString();
            if (!Arrays.asList("CASH", "PAYOS").contains(paymentMethod)) {
                throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ: " + paymentMethod);
            }

            for (Map<String, Object> item : items) {
                if (!item.containsKey("productId") || !item.containsKey("quantity") || !item.containsKey("price")) {
                    throw new IllegalArgumentException("Thông tin sản phẩm không hợp lệ");
                }
                try {
                    Integer.parseInt(item.get("productId").toString());
                    Integer.parseInt(item.get("quantity").toString());
                    Double.parseDouble(item.get("price").toString());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Dữ liệu sản phẩm không hợp lệ: " + item);
                }
            }

            // Fix: Safely handle null shippingAddress
            String shippingAddress = request.containsKey("shippingAddress") && request.get("shippingAddress") != null ?
                    request.get("shippingAddress").toString() : null;

            OrderDTO orderDTO = orderService.createInStoreOrder(userId, items, paymentMethod, shippingAddress);
            return ResponseEntity.ok(orderDTO);
        } catch (IllegalArgumentException e) {
            log.error("Invalid data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (ResponseStatusException e) {
            log.error("ResponseStatusException: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                    .body("Lỗi: " + e.getReason());
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tạo đơn hàng tại quầy: " + e.getMessage());
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