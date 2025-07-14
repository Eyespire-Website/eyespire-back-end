package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.CartDTO;
import org.eyespire.eyespireapi.dto.CartItemDTO;
import org.eyespire.eyespireapi.dto.OrderDTO;
import org.eyespire.eyespireapi.dto.ProductDTO;
import org.eyespire.eyespireapi.model.*;
import org.eyespire.eyespireapi.model.enums.OrderStatus;
import org.eyespire.eyespireapi.model.enums.PaymentStatus;
import org.eyespire.eyespireapi.model.enums.PaymentType;
import org.eyespire.eyespireapi.repository.OrderItemRepository;
import org.eyespire.eyespireapi.repository.OrderRepository;
import org.eyespire.eyespireapi.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Tạo đơn hàng từ giỏ hàng
     * @param userId ID của người dùng
     * @param shippingAddress Địa chỉ giao hàng
     * @return Thông tin đơn hàng đã tạo
     */
    @Transactional
    public OrderDTO createOrderFromCart(Long userId, String shippingAddress) {
        if (userId == null) {
            throw new IllegalArgumentException("ID người dùng không được để trống");
        }
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ giao hàng không được để trống");
        }

        // Lấy thông tin giỏ hàng của người dùng
        CartDTO cartDTO = cartService.getUserCartById(userId);
        if (cartDTO == null || cartDTO.getItems() == null || cartDTO.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng trống");
        }

        // Lấy thông tin người dùng
        User user = userService.getUserById(userId.intValue());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với ID: " + userId);
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
            if (cartItem.getProductId() == null || cartItem.getQuantity() <= 0) {
                throw new IllegalArgumentException("Dữ liệu mục giỏ hàng không hợp lệ");
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);

            // Fetch Product entity
            ProductDTO productDTO = productService.getProductById(cartItem.getProductId());
            if (productDTO == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + cartItem.getProductId());
            }
            Product product = productService.convertToEntity(productDTO);
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
     * Tạo đơn hàng tại quầy
     */
    @Transactional
    public OrderDTO createInStoreOrder(Long userId, List<Map<String, Object>> items, String paymentMethod, String shippingAddress) {
        log.info("Creating in-store order for userId: {}, items: {}, paymentMethod: {}", userId, items, paymentMethod);

        // Validate inputs
        if (userId == null) {
            log.error("userId is null");
            throw new IllegalArgumentException("ID người dùng không được để trống");
        }
        if (items == null || items.isEmpty()) {
            log.error("Items list is null or empty");
            throw new IllegalArgumentException("Danh sách sản phẩm không được để trống");
        }
        if (paymentMethod == null || !Arrays.asList("CASH", "PAYOS").contains(paymentMethod)) {
            log.error("Invalid paymentMethod: {}", paymentMethod);
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ: " + paymentMethod);
        }

        // Validate user
        User user = userService.getUserById(userId.intValue());
        if (user == null) {
            log.error("User not found for ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với ID: " + userId);
        }

        // Create order
        Order order = new Order();
        order.setPatient(user);
        order.setOrderDate(LocalDate.now());
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(shippingAddress);

        // Process items
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map<String, Object> item : items) {
            if (!item.containsKey("productId") || !item.containsKey("quantity") || !item.containsKey("price")) {
                log.error("Invalid item data: {}", item);
                throw new IllegalArgumentException("Thông tin sản phẩm không hợp lệ: " + item);
            }

            Integer productId;
            Integer quantity;
            BigDecimal price;

            try {
                productId = Integer.parseInt(item.get("productId").toString());
                quantity = Integer.parseInt(item.get("quantity").toString());
                price = new BigDecimal(item.get("price").toString());
            } catch (NumberFormatException e) {
                log.error("Invalid number format in item: {}", item);
                throw new IllegalArgumentException("Dữ liệu sản phẩm không hợp lệ: " + item);
            }

            if (quantity <= 0) {
                log.error("Invalid quantity: {}", quantity);
                throw new IllegalArgumentException("Số lượng sản phẩm phải lớn hơn 0");
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        log.error("Product not found for ID: {}", productId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + productId);
                    });

            if (product.getStockQuantity() < quantity) {
                log.error("Insufficient stock for product ID: {}, requested: {}, available: {}",
                        productId, quantity, product.getStockQuantity());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Số lượng sản phẩm " + product.getName() + " vượt quá tồn kho");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(price);
            orderItem.setSubtotal(price.multiply(BigDecimal.valueOf(quantity)));

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());

            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        // Create OrderPayment
        OrderPayment payment = new OrderPayment();
        payment.setOrder(order);
        payment.setAmount(totalAmount);
        payment.setTransactionNo(UUID.randomUUID().toString());
        payment.setStatus(paymentMethod.equals("CASH") ? PaymentStatus.COMPLETED : PaymentStatus.PENDING);
        payment.setPaymentType(paymentMethod.equals("CASH") ? PaymentType.CASH : PaymentType.PAYOS);
        payment.setPaymentDate(LocalDateTime.now());

        order.setPayment(payment);

        // Save order
        Order savedOrder = orderRepository.save(order);
        log.info("In-store order created successfully: {}", savedOrder.getId());

        if (paymentMethod.equals("CASH")) {
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
        }

        return convertToDTO(savedOrder);
    }

    /**
     * Lấy thông tin đơn hàng theo ID
     * @param orderId ID của đơn hàng
     * @return Thông tin đơn hàng
     */
    public OrderDTO getOrderById(Integer orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("ID đơn hàng không được để trống");
        }
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng với ID: " + orderId));
        return convertToDTO(order);
    }

    /**
     * Lấy danh sách đơn hàng của người dùng
     * @param userId ID của người dùng
     * @return Danh sách đơn hàng
     */
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("ID người dùng không được để trống");
        }
        List<Order> orders = orderRepository.findByPatientIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Lấy tất cả đơn hàng
     * @return Danh sách tất cả đơn hàng
     */
    public List<OrderDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Cập nhật trạng thái đơn hàng
     * @param id ID của đơn hàng
     * @param status Trạng thái mới
     * @return Thông tin đơn hàng đã cập nhật
     */
    @Transactional
    public OrderDTO updateOrderStatus(Integer id, String status) {
        log.info("Updating order status: id={}, status={}", id, status);
        if (id == null) {
            log.error("Order ID is null");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID đơn hàng không được để trống");
        }
        Order order = orderRepository.findById(id).orElseThrow(() -> {
            log.error("Order not found for ID: {}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng với ID: " + id);
        });
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            order.setStatus(orderStatus);
            log.info("Successfully set status to {} for order {}", status, id);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}. Valid statuses: {}", status, Arrays.toString(OrderStatus.values()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ: " + status + ". Các giá trị hợp lệ: " + Arrays.toString(OrderStatus.values()));
        }
        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} updated successfully", id);
        return convertToDTO(updatedOrder);
    }


    /**
     * Cập nhật đơn hàng
     * @param orderDTO Thông tin đơn hàng cần cập nhật
     * @return Thông tin đơn hàng đã cập nhật
     */
    @Transactional
    public OrderDTO updateOrder(OrderDTO orderDTO) {
        if (orderDTO == null || orderDTO.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thông tin đơn hàng hoặc ID không hợp lệ");
        }

        Order order = orderRepository.findById(orderDTO.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng với ID: " + orderDTO.getId()));

        if (orderDTO.getUserId() != null) {
            User user = userService.getUserById(orderDTO.getUserId().intValue());
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với ID: " + orderDTO.getUserId());
            }
            order.setPatient(user);
        }

        if (orderDTO.getShippingAddress() != null && !orderDTO.getShippingAddress().trim().isEmpty()) {
            order.setShippingAddress(orderDTO.getShippingAddress());
        }

        if (orderDTO.getStatus() != null) {
            try {
                order.setStatus(OrderStatus.valueOf(orderDTO.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái đơn hàng không hợp lệ: " + orderDTO.getStatus());
            }
        }

        if (orderDTO.getItems() != null) {
            order.getOrderItems().clear();
            List<OrderItem> newItems = orderDTO.getItems().stream().map(itemDTO -> {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                ProductDTO productDTO = productService.getProductById(itemDTO.getProductId());
                Product product = productService.convertToEntity(productDTO);
                orderItem.setProduct(product);
                orderItem.setQuantity(itemDTO.getQuantity());
                orderItem.setPrice(itemDTO.getPrice());
                orderItem.setSubtotal(itemDTO.getSubtotal());
                return orderItem;
            }).collect(Collectors.toList());
            order.getOrderItems().addAll(newItems);
        }

        if (orderDTO.getTotalAmount() != null) {
            order.setTotalAmount(orderDTO.getTotalAmount());
        }

        if (orderDTO.getOrderDate() != null) {
            order.setOrderDate(orderDTO.getOrderDate());
        }

        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    /**
     * Deletes an order by ID
     * @param id The ID of the order to delete
     * @throws IllegalArgumentException if the order is not found
     */
    @Transactional
    public void deleteOrder(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID đơn hàng không hợp lệ");
        }
        if (!orderRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng với ID: " + id);
        }
        orderRepository.deleteById(id);
    }

    /**
     * Chuyển đổi từ Entity sang DTO
     * @param order Entity
     * @return DTO
     */
    private OrderDTO convertToDTO(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Đơn hàng không được để trống");
        }
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getPatient() != null ? Long.valueOf(order.getPatient().getId()) : null);
        dto.setTotalAmount(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
        dto.setStatus(order.getStatus() != null ? order.getStatus().toString() : OrderStatus.PENDING.toString());
        dto.setOrderDate(order.getOrderDate());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setCreatedAt(order.getCreatedAt());

        if (order.getPayment() != null) {
            OrderDTO.PaymentDTO paymentDTO = new OrderDTO.PaymentDTO();
            paymentDTO.setId(order.getPayment().getId());
            paymentDTO.setTransactionNo(order.getPayment().getTransactionNo());
            paymentDTO.setAmount(order.getPayment().getAmount());
            paymentDTO.setStatus(order.getPayment().getStatus() != null ? order.getPayment().getStatus().toString() : PaymentStatus.PENDING.toString());
            paymentDTO.setPaymentDate(order.getPayment().getPaymentDate());
            dto.setPayment(paymentDTO);
        }

        List<OrderDTO.OrderItemDTO> itemDTOs = order.getOrderItems() != null ?
                order.getOrderItems().stream().map(item -> {
                    OrderDTO.OrderItemDTO itemDTO = new OrderDTO.OrderItemDTO();
                    itemDTO.setId(item.getId());
                    itemDTO.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
                    itemDTO.setProductName(item.getProduct() != null ? item.getProduct().getName() : "Không xác định");
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setPrice(item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
                    itemDTO.setSubtotal(item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO);
                    itemDTO.setImage(item.getProduct() != null ? item.getProduct().getImageUrl() : null);
                    return itemDTO;
                }).collect(Collectors.toList()) : new ArrayList<>();

        dto.setItems(itemDTOs);
        return dto;
    }
}