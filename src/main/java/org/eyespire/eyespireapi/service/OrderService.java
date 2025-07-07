package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.CartDTO;
import org.eyespire.eyespireapi.dto.CartItemDTO;
import org.eyespire.eyespireapi.dto.OrderDTO;
import org.eyespire.eyespireapi.model.*;
import org.eyespire.eyespireapi.model.enums.OrderStatus;
import org.eyespire.eyespireapi.model.enums.PaymentStatus;
import org.eyespire.eyespireapi.repository.OrderItemRepository;
import org.eyespire.eyespireapi.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    /**
     * Tạo đơn hàng từ giỏ hàng
     * @param userId ID của người dùng
     * @param shippingAddress Địa chỉ giao hàng
     * @return Thông tin đơn hàng đã tạo
     */
    @Transactional
    public OrderDTO createOrderFromCart(Long userId, String shippingAddress) {
        // Lấy thông tin giỏ hàng của người dùng
        CartDTO cartDTO = cartService.getUserCartById(userId);
        if (cartDTO == null || cartDTO.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng trống");
        }

        // Lấy thông tin người dùng
        User user = userService.getUserById(userId.intValue());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        }

        // Tạo đơn hàng mới
        Order order = new Order();
        order.setPatient(user);
        order.setOrderDate(LocalDate.now());
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(shippingAddress);

        // Tính tổng tiền và tạo các mục đơn hàng
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemDTO cartItem : cartDTO.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            
            // Tạo đối tượng Product với ID
            Product product = new Product();
            product.setId(cartItem.getProductId());
            orderItem.setProduct(product);
            
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(BigDecimal.valueOf(cartItem.getPrice()));
            orderItem.setSubtotal(BigDecimal.valueOf(cartItem.getPrice() * cartItem.getQuantity()));
            
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        // Lưu đơn hàng vào database
        Order savedOrder = orderRepository.save(order);

        // Xóa giỏ hàng sau khi đã tạo đơn hàng
        cartService.clearCartByUserId(userId.intValue());

        // Chuyển đổi sang DTO và trả về
        return convertToDTO(savedOrder);
    }

    /**
     * Lấy thông tin đơn hàng theo ID
     * @param orderId ID của đơn hàng
     * @return Thông tin đơn hàng
     */
    public OrderDTO getOrderById(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        return convertToDTO(order);
    }

    /**
     * Lấy danh sách đơn hàng của người dùng
     * @param userId ID của người dùng
     * @return Danh sách đơn hàng
     */
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByPatientIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Chuyển đổi từ Entity sang DTO
     * @param order Entity
     * @return DTO
     */
    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(Long.valueOf(order.getPatient().getId()));
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().toString());
        dto.setOrderDate(order.getOrderDate());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setCreatedAt(order.getCreatedAt());
        
        // Thông tin thanh toán
        if (order.getPayment() != null) {
            OrderDTO.PaymentDTO paymentDTO = new OrderDTO.PaymentDTO();
            paymentDTO.setId(order.getPayment().getId());
            paymentDTO.setTransactionNo(order.getPayment().getTransactionNo());
            paymentDTO.setAmount(order.getPayment().getAmount());
            paymentDTO.setStatus(order.getPayment().getStatus().toString());
            paymentDTO.setPaymentDate(order.getPayment().getPaymentDate());
            dto.setPayment(paymentDTO);
        }
        
        // Các mục đơn hàng
        List<OrderDTO.OrderItemDTO> itemDTOs = order.getOrderItems().stream().map(item -> {
            OrderDTO.OrderItemDTO itemDTO = new OrderDTO.OrderItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setProductId(item.getProduct().getId());
            itemDTO.setProductName(item.getProduct().getName());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setPrice(item.getPrice());
            itemDTO.setSubtotal(item.getSubtotal());
            return itemDTO;
        }).collect(Collectors.toList());
        
        dto.setItems(itemDTOs);
        
        return dto;
    }
}
