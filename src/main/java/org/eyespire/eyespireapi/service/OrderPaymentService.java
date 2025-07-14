package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.AppointmentDTO;
import org.eyespire.eyespireapi.dto.OrderDTO;
import org.eyespire.eyespireapi.dto.OrderPayOSCreateRequest;
import org.eyespire.eyespireapi.dto.OrderPayOSCreateResponse;
import org.eyespire.eyespireapi.dto.PayOSVerifyResponse;
import org.eyespire.eyespireapi.model.Order;
import org.eyespire.eyespireapi.model.OrderPayment;
import org.eyespire.eyespireapi.model.Payment;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.OrderStatus;
import org.eyespire.eyespireapi.model.enums.PaymentStatus;
import org.eyespire.eyespireapi.repository.OrderPaymentRepository;
import org.eyespire.eyespireapi.repository.OrderRepository;
import org.eyespire.eyespireapi.repository.PaymentRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class OrderPaymentService {

    @Value("${payos.clientId}")
    private String clientId;

    @Value("${payos.apiKey}")
    private String apiKey;

    @Value("${payos.checksumKey}")
    private String checksumKey;

    @Value("${payos.apiUrl}")
    private String apiUrl;

    @Autowired
    private OrderPaymentRepository orderPaymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Tạo thanh toán cho đơn hàng
     *
     * @param order Đơn hàng cần thanh toán
     * @param user  Người dùng thực hiện thanh toán
     * @param customReturnUrl URL trả về tùy chỉnh (có thể null)
     * @return Thông tin thanh toán
     */
    @Transactional
    public OrderPayOSCreateResponse createOrderPayment(Order order, User user, String customReturnUrl) {
        try {
            // Tạo mã giao dịch - sử dụng orderId kết hợp với timestamp ngắn hơn để đảm bảo không vượt quá giới hạn
            // Lấy 6 chữ số cuối của timestamp hiện tại
            int shortTimestamp = (int)(System.currentTimeMillis() % 1000000);
            // Kết hợp orderId và shortTimestamp để tạo mã giao dịch duy nhất
            int transactionNo = order.getId() * 1000000 + shortTimestamp;

            // Tạo đối tượng OrderPayment
            OrderPayment payment = new OrderPayment();
            payment.setTransactionNo(String.valueOf(transactionNo));
            payment.setAmount(order.getTotalAmount());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setOrder(order);

            // Lưu vào database
            payment = orderPaymentRepository.save(payment);

            // Tạo metadata cho PayOS
            Map<String, String> metadata = new HashMap<>();
            metadata.put("userId", String.valueOf(user.getId()));
            metadata.put("patientName", user.getName());
            metadata.put("patientEmail", user.getEmail());
            metadata.put("patientPhone", user.getPhone());
            metadata.put("notes", "Thanh toán đơn hàng #" + order.getId());

            // Tạo dữ liệu đơn hàng
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orderId", order.getId());
            orderData.put("totalAmount", order.getTotalAmount());
            orderData.put("orderDate", order.getOrderDate());

            // Tạo dữ liệu lịch hẹn (để tương thích với luồng thanh toán lịch hẹn)
            AppointmentDTO appointmentDTO = new AppointmentDTO();
            appointmentDTO.setUserId(user.getId());
            appointmentDTO.setPatientName(user.getName());
            appointmentDTO.setPatientEmail(user.getEmail());
            appointmentDTO.setPatientPhone(user.getPhone());
            appointmentDTO.setAppointmentDate(LocalDate.now().plusDays(1).toString());
            appointmentDTO.setTimeSlot("08:00");

            // Định nghĩa URL trả về và URL hủy
            String returnUrl = customReturnUrl != null ? customReturnUrl : "http://localhost:3000/payment/order-return";
            String cancelUrl = "http://localhost:3000/payment/order-cancel";

            // Tạo yêu cầu thanh toán
            OrderPayOSCreateRequest paymentRequest = OrderPayOSCreateRequest.builder()
                    .orderCode(String.valueOf(transactionNo))
                    .amount(payment.getAmount())
                    .description("Dat coc kham Eyespire")
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .buyerName(user.getName())
                    .buyerEmail(user.getEmail())
                    .buyerPhone(user.getPhone())
                    .metadata(metadata)
                    .orderData(orderData)
                    .appointmentData(appointmentDTO)
                    .build();

            // Gọi phương thức createPayOSPayment để tạo thanh toán qua PayOS API
            OrderPayOSCreateResponse response = createPayOSPayment(paymentRequest);
            
            // Cập nhật payosTransactionId nếu có
            if (response != null && response.getPayosTransactionId() != null) {
                payment.setPayosTransactionId(response.getPayosTransactionId());
                orderPaymentRepository.save(payment);
            }
            
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo thanh toán: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo thanh toán qua PayOS API
     *
     * @param request Yêu cầu thanh toán
     * @return Kết quả tạo thanh toán
     */
    private OrderPayOSCreateResponse createPayOSPayment(OrderPayOSCreateRequest request) {
        try {
            if (request.getReturnUrl() == null || request.getReturnUrl().isEmpty()) {
                throw new IllegalArgumentException("URL trả về không được để trống");
            }
            
            // Sử dụng orderCode từ request hoặc tạo mới nếu không có
            String orderCode = request.getOrderCode();
            if (orderCode == null || orderCode.isEmpty()) {
                orderCode = String.valueOf(generateOrderCode());
            }
            
            // Tạo request body cho PayOS API v2
            Map<String, Object> paymentData = new HashMap<>();
            
            // Chuyển đổi orderCode từ chuỗi sang số nguyên
            try {
                // Chuyển đổi orderCode từ chuỗi sang số nguyên
                long orderCodeNum = Long.parseLong(orderCode);
                // Kiểm tra giới hạn của PayOS
                if (orderCodeNum <= 0 || orderCodeNum > 9007199254740991L) {
                    // Nếu vượt quá giới hạn, tạo mã mới
                    orderCodeNum = generateOrderCode();
                }
                // Gửi orderCode dưới dạng số nguyên, không phải chuỗi
                paymentData.put("orderCode", orderCodeNum);
            } catch (NumberFormatException e) {
                // Nếu không thể chuyển đổi, tạo mã mới
                long orderCodeNum = generateOrderCode();
                paymentData.put("orderCode", orderCodeNum);
                // Cập nhật orderCode để sử dụng trong các bước tiếp theo
                orderCode = String.valueOf(orderCodeNum);
            }
            
            paymentData.put("amount", request.getAmount().intValue());
            String description = request.getDescription() != null ? request.getDescription() : "Dat coc kham Eyespire";
            paymentData.put("description", description);
            String returnUrl = request.getReturnUrl();
            paymentData.put("returnUrl", returnUrl);
            String cancelUrl = request.getCancelUrl() != null ? request.getCancelUrl() : (returnUrl + "?status=cancel");
            paymentData.put("cancelUrl", cancelUrl);
            
            // Thêm thông tin người mua nếu có
            if (request.getBuyerName() != null) {
                paymentData.put("buyerName", request.getBuyerName());
            }
            if (request.getBuyerEmail() != null) {
                paymentData.put("buyerEmail", request.getBuyerEmail());
            }
            if (request.getBuyerPhone() != null) {
                paymentData.put("buyerPhone", request.getBuyerPhone());
            }
            
            // Thêm metadata từ request nếu có
            if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
                paymentData.put("metadata", request.getMetadata());
            } 
            
            // Thêm thông tin đơn hàng nếu có
            if (request.getOrderData() != null) {
                paymentData.put("orderData", request.getOrderData());
            }
            
            // Tạo chữ ký
            String signature = generateSignature(
                    orderCode, 
                    String.valueOf(request.getAmount().intValue()), 
                    description, 
                    returnUrl, 
                    cancelUrl
            );
            paymentData.put("signature", signature);
            
            // Tạo header với thông tin xác thực
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            
            // Tạo request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(paymentData, headers);
            
            System.out.println("Gửi yêu cầu tạo thanh toán đến PayOS: " + paymentData);
            
            // Gọi API PayOS để tạo thanh toán
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/payment-requests",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            // Xử lý response
            Map<String, Object> responseBody = response.getBody();
            System.out.println("Phản hồi từ PayOS: " + responseBody);
            
            if (responseBody != null && responseBody.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                String checkoutUrl = (String) data.get("checkoutUrl");
                String payosTransactionId = (String) data.get("id");
                
                // Tạo response
                OrderPayOSCreateResponse paymentResponse = new OrderPayOSCreateResponse();
                paymentResponse.setSuccess(true);
                paymentResponse.setMessage("Tạo thanh toán thành công");
                paymentResponse.setCheckoutUrl(checkoutUrl);
                paymentResponse.setTransactionNo(orderCode);
                paymentResponse.setPayosTransactionId(payosTransactionId);
                
                return paymentResponse;
            } else {
                throw new RuntimeException("Không thể tạo thanh toán: " + responseBody);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo thanh toán đơn hàng: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo thanh toán đơn hàng: " + e.getMessage(), e);
        }
    }
    
    /**
     * Tạo mã đơn hàng duy nhất
     * @return Mã đơn hàng duy nhất
     */
    private long generateOrderCode() {
        // Tạo mã đơn hàng duy nhất dựa trên timestamp
        // Đảm bảo nằm trong giới hạn của PayOS (dưới 9007199254740991)
        return System.currentTimeMillis() % 9007199254740991L;
    }
    
    /**
     * Tạo chữ ký cho PayOS API v2
     */
    private String generateSignature(String orderCode, String amount, String description, String returnUrl, String cancelUrl) {
        try {
            // Tạo chuỗi dữ liệu cần ký theo định dạng của PayOS
            // Format: amount=$amount&cancelUrl=$cancelUrl&description=$description&orderCode=$orderCode&returnUrl=$returnUrl
            String dataToSign = "amount=" + amount + 
                              "&cancelUrl=" + cancelUrl + 
                              "&description=" + description + 
                              "&orderCode=" + orderCode + 
                              "&returnUrl=" + returnUrl;
            
            // Tạo chữ ký HMAC-SHA256
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(checksumKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            
            // Tạo chữ ký
            byte[] hash = sha256_HMAC.doFinal(dataToSign.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // Chuyển đổi sang chuỗi hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo chữ ký: " + e.getMessage(), e);
        }
    }

    /**
     * Xác thực kết quả thanh toán từ PayOS
     *
     * @param params Các tham số từ PayOS callback
     * @return Kết quả xác thực
     */
    @Transactional
    public PayOSVerifyResponse verifyOrderPayment(Map<String, String> params) {
        try {
            System.out.println("Bắt đầu xác thực thanh toán đơn hàng với params: " + params);

            // Lấy thông tin từ callback
            String orderCode = params.get("orderCode");
            String status = params.get("status");
            String payosId = params.get("id");
            
            System.out.println("Xác thực thanh toán với orderCode=" + orderCode + ", status=" + status + ", payosId=" + payosId);
            
            if (orderCode == null) {
                return PayOSVerifyResponse.builder()
                        .success(false)
                        .message("Thiếu mã giao dịch")
                        .build();
            }
            
            // Tìm thanh toán trong database - chỉ tìm trong bảng order_payments
            Optional<OrderPayment> orderPaymentOpt = orderPaymentRepository.findByTransactionNo(orderCode);
            
            if (!orderPaymentOpt.isPresent()) {
                System.out.println("Không tìm thấy thanh toán đơn hàng với transactionNo: " + orderCode);
                return PayOSVerifyResponse.builder()
                        .success(false)
                        .message("Không tìm thấy giao dịch đơn hàng")
                        .build();
            }
            
            OrderPayment payment = orderPaymentOpt.get();
            Order order = payment.getOrder();
            
            if (order == null) {
                System.out.println("CẢNH BÁO: Không tìm thấy đơn hàng liên kết với thanh toán ID: " + payment.getId());
                return PayOSVerifyResponse.builder()
                        .success(false)
                        .message("Không tìm thấy đơn hàng liên kết")
                        .build();
            }
            
            System.out.println("Đơn hàng liên kết có ID: " + order.getId() + ", trạng thái hiện tại: " + order.getStatus());
            
            // Kiểm tra trạng thái từ PayOS
            if ("PAID".equals(status)) {
                // Xác thực giao dịch với PayOS API
                Map<String, Object> paymentInfo = getPaymentInfoFromPayOS(payosId);
                System.out.println("Phản hồi xác thực từ PayOS: " + paymentInfo);
                
                if (paymentInfo != null && paymentInfo.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) paymentInfo.get("data");
                    
                    // Kiểm tra trạng thái từ API
                    String apiStatus = data.containsKey("status") ? data.get("status").toString() : "";
                    
                    if ("PAID".equals(apiStatus)) {
                        // Cập nhật trạng thái thanh toán
                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setPaymentDate(LocalDateTime.now());
                        
                        // Cập nhật thông tin từ PayOS
                        if (payosId != null) {
                            payment.setPayosTransactionId(payosId);
                        }
                        
                        // Cập nhật trạng thái đơn hàng
                        order.setStatus(OrderStatus.PAID);
                        
                        // Lưu vào database
                        orderPaymentRepository.save(payment);
                        orderRepository.save(order);
                        
                        System.out.println("Đã cập nhật trạng thái thanh toán thành COMPLETED và đơn hàng thành PAID");
                        
                        // Tạo response
                        return PayOSVerifyResponse.builder()
                                .success(true)
                                .message("Thanh toán đơn hàng thành công")
                                .status(status)
                                .paymentId(payment.getId())
                                .transactionNo(orderCode)
                                .amount(payment.getAmount())
                                .paymentDate(LocalDateTime.now())
                                .payosTransactionId(payosId)
                                .build();
                    } else {
                        System.out.println("Trạng thái từ PayOS API không phải PAID: " + apiStatus);
                    }
                } else {
                    System.out.println("Không nhận được dữ liệu hợp lệ từ PayOS API");
                }
            } else {
                System.out.println("Thanh toán không thành công hoặc chưa hoàn tất. Trạng thái: " + status);
            }
            
            // Trả về response mặc định nếu không phải trạng thái PAID
            return PayOSVerifyResponse.builder()
                    .success("PAID".equals(status))
                    .message("PAID".equals(status) ? "Thanh toán đơn hàng thành công" : "Thanh toán đơn hàng chưa hoàn tất")
                    .status(status)
                    .paymentId(payment.getId())
                    .transactionNo(orderCode)
                    .amount(payment.getAmount())
                    .paymentDate(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            System.out.println("Lỗi khi xác thực thanh toán đơn hàng: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi xác thực thanh toán đơn hàng: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lấy thông tin chi tiết thanh toán từ PayOS API
     *
     * @param payosTransactionId ID giao dịch PayOS
     * @return Thông tin chi tiết thanh toán
     */
    private Map<String, Object> getPaymentInfoFromPayOS(String payosTransactionId) {
        try {
            // Tạo headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            
            // Tạo request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            
            // Gọi API PayOS để lấy thông tin chi tiết
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/payment-requests/" + payosTransactionId,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông tin thanh toán từ PayOS: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán
     *
     * @param orderCode Mã đơn hàng
     * @return Trạng thái thanh toán
     */
    public Map<String, Object> checkPaymentStatus(String orderCode) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Tìm thanh toán theo mã giao dịch
            Optional<OrderPayment> paymentOpt = orderPaymentRepository.findByTransactionNo(orderCode);

            if (paymentOpt.isPresent()) {
                OrderPayment payment = paymentOpt.get();
                Order order = payment.getOrder();

                result.put("success", true);
                result.put("paymentStatus", payment.getStatus().toString());
                result.put("orderStatus", order.getStatus().toString());
                result.put("orderId", order.getId());
                result.put("amount", payment.getAmount());
                result.put("paymentDate", payment.getPaymentDate());
            } else {
                result.put("success", false);
                result.put("message", "Không tìm thấy thông tin thanh toán");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Lỗi khi kiểm tra trạng thái thanh toán: " + e.getMessage());
        }

        return result;
    }

    /**
     * Kiểm tra trạng thái thanh toán của đơn hàng
     *
     * @param orderId ID của đơn hàng
     * @return Thông tin trạng thái thanh toán
     */
    public Map<String, Object> checkOrderPaymentStatus(Integer orderId) {
        try {
            // Tìm đơn hàng theo ID
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (!orderOpt.isPresent()) {
                throw new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId);
            }

            Order order = orderOpt.get();

            // Tìm thanh toán mới nhất của đơn hàng
            Optional<OrderPayment> paymentOpt = orderPaymentRepository.findTopByOrderOrderByIdDesc(order);

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", orderId);
            result.put("orderStatus", order.getStatus().toString());

            if (paymentOpt.isPresent()) {
                OrderPayment payment = paymentOpt.get();
                result.put("paymentId", payment.getId());
                result.put("paymentStatus", payment.getStatus().toString());
                result.put("amount", payment.getAmount());
                result.put("transactionNo", payment.getTransactionNo());
                result.put("paymentDate", payment.getPaymentDate());
                result.put("payosTransactionId", payment.getPayosTransactionId());
                result.put("isPaid", OrderStatus.PAID.equals(order.getStatus()));
            } else {
                result.put("paymentStatus", "NOT_FOUND");
                result.put("isPaid", false);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi kiểm tra trạng thái thanh toán: " + e.getMessage(), e);
        }
    }
}
