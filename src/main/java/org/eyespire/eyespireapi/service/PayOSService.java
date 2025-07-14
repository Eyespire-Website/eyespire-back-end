package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.AppointmentDTO;
import org.eyespire.eyespireapi.dto.OrderPayOSCreateRequest;
import org.eyespire.eyespireapi.dto.OrderPayOSCreateResponse;
import org.eyespire.eyespireapi.dto.PayOSCreateRequest;
import org.eyespire.eyespireapi.dto.PayOSCreateResponse;
import org.eyespire.eyespireapi.dto.PayOSVerifyResponse;
import org.eyespire.eyespireapi.model.Payment;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.PaymentStatus;
import org.eyespire.eyespireapi.repository.PaymentRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

@Service
public class PayOSService {

    @Value("${payos.clientId}")
    private String clientId;

    @Value("${payos.apiKey}")
    private String apiKey;

    @Value("${payos.checksumKey}")
    private String checksumKey;

    @Value("${payos.apiUrl}")
    private String apiUrl;

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AppointmentService appointmentService;
    
    @Autowired
    private RestTemplate restTemplate;

    public PayOSCreateResponse createPayment(PayOSCreateRequest request) {
        try {
            // Kiểm tra dữ liệu đầu vào
            if (request == null || request.getAmount() == null || request.getAmount().intValue() <= 0) {
                throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ");
            }
            
            if (request.getReturnUrl() == null || request.getReturnUrl().isEmpty()) {
                throw new IllegalArgumentException("URL trả về không được để trống");
            }
            
            // Tạo mã giao dịch dạng số
            long transactionNo = generateOrderCode();
            
            // Lưu thông tin thanh toán vào database
            Payment payment = new Payment();
            payment.setTransactionNo(String.valueOf(transactionNo)); // Lưu dưới dạng chuỗi trong DB
            payment.setAmount(request.getAmount());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setReturnUrl(request.getReturnUrl());
            payment.setPayosOrderInfo("Dat coc lich kham");
            
            // Lưu thông tin đặt lịch vào Payment
            if (request.getAppointmentData() != null) {
                AppointmentDTO appointmentData = request.getAppointmentData();
                
                if (appointmentData.getDoctorId() != null) {
                    payment.setDoctorId(appointmentData.getDoctorId());
                }
                
                if (appointmentData.getServiceId() != null) {
                    payment.setServiceId(appointmentData.getServiceId());
                }
                
                if (appointmentData.getAppointmentDate() != null) {
                    payment.setAppointmentDate(appointmentData.getAppointmentDate());
                }
                
                if (appointmentData.getTimeSlot() != null) {
                    payment.setTimeSlot(appointmentData.getTimeSlot());
                }
                
                if (appointmentData.getUserId() != null) {
                    payment.setUserId(appointmentData.getUserId());
                }
                
                if (appointmentData.getPatientName() != null) {
                    System.out.println("[UTF-8 DEBUG] createPayment - patientName: " + appointmentData.getPatientName());
                    // Vietnamese text is already properly encoded as UTF-8 from frontend
                    payment.setPatientName(appointmentData.getPatientName());
                }
                
                if (appointmentData.getPatientEmail() != null) {
                    payment.setPatientEmail(appointmentData.getPatientEmail());
                }
                
                if (appointmentData.getPatientPhone() != null) {
                    payment.setPatientPhone(appointmentData.getPatientPhone());
                }
                
                if (appointmentData.getNotes() != null) {
                    System.out.println("[UTF-8 DEBUG] createPayment - notes: " + appointmentData.getNotes());
                    // Vietnamese text is already properly encoded as UTF-8 from frontend
                    payment.setNotes(appointmentData.getNotes());
                }
            }
            
            payment = paymentRepository.save(payment);
            
            // Tạo request body cho PayOS API v2
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("orderCode", transactionNo); // Gửi dạng số cho PayOS
            paymentData.put("amount", payment.getAmount().intValue());
            String description = "Dat coc kham Eyespire"; // Rút ngắn xuống 25 ký tự
            paymentData.put("description", description);
            String returnUrl = payment.getReturnUrl();
            paymentData.put("returnUrl", returnUrl);
            String cancelUrl = request.getReturnUrl() + "?status=cancel";
            paymentData.put("cancelUrl", cancelUrl);
            
            // Thêm thông tin bệnh nhân vào metadata
            Map<String, Object> metadata = new HashMap<>();
            if (request.getAppointmentData() != null) {
                AppointmentDTO appointmentData = request.getAppointmentData();
                
                if (appointmentData.getPatientName() != null) {
                    metadata.put("patientName", appointmentData.getPatientName());
                }
                
                if (appointmentData.getPatientPhone() != null) {
                    metadata.put("patientPhone", appointmentData.getPatientPhone());
                }
                
                if (appointmentData.getDoctorId() != null) {
                    metadata.put("doctorId", appointmentData.getDoctorId());
                }
                
                if (appointmentData.getServiceId() != null) {
                    metadata.put("serviceId", appointmentData.getServiceId());
                }
                
                // Thêm ngày hẹn
                if (appointmentData.getAppointmentDate() != null) {
                    metadata.put("appointmentDate", appointmentData.getAppointmentDate());
                }
                
                // Thêm khung giờ
                if (appointmentData.getTimeSlot() != null) {
                    metadata.put("timeSlot", appointmentData.getTimeSlot());
                }
                
                // Thêm userId nếu có
                if (appointmentData.getUserId() != null) {
                    metadata.put("userId", appointmentData.getUserId());
                }
                
                // Thêm email nếu có
                if (appointmentData.getPatientEmail() != null) {
                    metadata.put("patientEmail", appointmentData.getPatientEmail());
                }
                
                // Thêm ghi chú nếu có
                if (appointmentData.getNotes() != null) {
                    metadata.put("notes", appointmentData.getNotes());
                }
            }
            
            // Thêm metadata vào paymentData
            paymentData.put("metadata", metadata);
            
            paymentData.put("items", new ArrayList<>()); // Thêm danh sách items rỗng theo yêu cầu của PayOS v2
            paymentData.put("currency", "VND"); // Thêm loại tiền tệ theo yêu cầu của PayOS v2
            
            // Tính signature theo đúng định dạng của PayOS
            String signature = generateSignature(
                String.valueOf(payment.getTransactionNo()), 
                String.valueOf(payment.getAmount().intValue()), 
                description,
                returnUrl,
                cancelUrl
            );
            paymentData.put("signature", signature);
            
            // Thêm thông tin người mua
            if (request.getAppointmentData() != null) {
                paymentData.put("buyerName", request.getAppointmentData().getPatientName() != null ? 
                    request.getAppointmentData().getPatientName() : "");
                paymentData.put("buyerPhone", request.getAppointmentData().getPatientPhone() != null ? 
                    request.getAppointmentData().getPatientPhone() : "");
                paymentData.put("buyerEmail", request.getAppointmentData().getPatientEmail() != null ? 
                    request.getAppointmentData().getPatientEmail() : "");
            }
            
            try {
                // Gọi API PayOS để tạo link thanh toán
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("x-client-id", clientId);
                headers.set("x-api-key", apiKey);
                
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(paymentData, headers);
                
                System.out.println("Gửi yêu cầu đến PayOS: " + paymentData);
                
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        apiUrl + "/payment-requests", 
                        requestEntity, 
                        Map.class
                );
                
                // Xử lý response từ PayOS
                Map<String, Object> responseBody = response.getBody();
                System.out.println("Phản hồi từ PayOS: " + responseBody);
                
                if (responseBody != null && responseBody.containsKey("data") && responseBody.get("data") != null) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    
                    // Kiểm tra các trường cần thiết trong data
                    if (data.containsKey("checkoutUrl") && data.containsKey("paymentLinkId")) {
                        String paymentUrl = (String) data.get("checkoutUrl");
                        String payosTransactionId = (String) data.get("paymentLinkId");
                        
                        // Cập nhật payment với thông tin từ PayOS
                        payment.setPayosTransactionId(payosTransactionId);
                        paymentRepository.save(payment);
                        
                        // Trả về thông tin thanh toán
                        return new PayOSCreateResponse(
                            payment.getId(),
                            paymentUrl,
                            payment.getTransactionNo()
                        );
                    } else {
                        throw new RuntimeException("Phản hồi từ PayOS thiếu thông tin cần thiết: " + data);
                    }
                } else {
                    // Xử lý khi data là null hoặc không tồn tại
                    String errorMessage = "Không thể tạo thanh toán PayOS";
                    if (responseBody != null) {
                        if (responseBody.containsKey("desc")) {
                            errorMessage += ": " + responseBody.get("desc");
                        } else if (responseBody.containsKey("message")) {
                            errorMessage += ": " + responseBody.get("message");
                        } else {
                            errorMessage += ": " + responseBody;
                        }
                    }
                    throw new RuntimeException(errorMessage);
                }
            } catch (Exception e) {
                // Xóa payment đã tạo nếu gọi API thất bại
                paymentRepository.delete(payment);
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace(); // In ra stack trace để dễ debug
            throw new RuntimeException("Lỗi khi tạo thanh toán PayOS: " + e.getMessage(), e);
        }
    }
    
    /**
     * Tạo thanh toán đơn hàng qua PayOS
     * Phương thức này riêng biệt cho đơn hàng và không ảnh hưởng đến luồng thanh toán lịch hẹn
     */
    public OrderPayOSCreateResponse createOrderPayment(OrderPayOSCreateRequest request) {
        try {
            // Kiểm tra dữ liệu đầu vào
            if (request == null || request.getAmount() == null || request.getAmount().intValue() <= 0) {
                throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ");
            }
            
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
            // Nếu không có metadata trong request, tạo từ appointmentData
            else if (request.getAppointmentData() != null) {
                Map<String, Object> metadata = new HashMap<>();
                AppointmentDTO appointmentData = request.getAppointmentData();
                
                if (appointmentData.getPatientName() != null) {
                    metadata.put("patientName", appointmentData.getPatientName());
                }
                
                if (appointmentData.getPatientPhone() != null) {
                    metadata.put("patientPhone", appointmentData.getPatientPhone());
                }
                
                if (appointmentData.getDoctorId() != null) {
                    metadata.put("doctorId", appointmentData.getDoctorId());
                }
                
                if (appointmentData.getServiceId() != null) {
                    metadata.put("serviceId", appointmentData.getServiceId());
                }
                
                // Thêm ngày hẹn
                if (appointmentData.getAppointmentDate() != null) {
                    metadata.put("appointmentDate", appointmentData.getAppointmentDate());
                }
                
                // Thêm khung giờ
                if (appointmentData.getTimeSlot() != null) {
                    metadata.put("timeSlot", appointmentData.getTimeSlot());
                }
                
                // Thêm userId nếu có
                if (appointmentData.getUserId() != null) {
                    metadata.put("userId", appointmentData.getUserId());
                }
                
                // Thêm email nếu có
                if (appointmentData.getPatientEmail() != null) {
                    metadata.put("patientEmail", appointmentData.getPatientEmail());
                }
                
                // Thêm ghi chú nếu có
                if (appointmentData.getNotes() != null) {
                    metadata.put("notes", appointmentData.getNotes());
                }
                
                paymentData.put("metadata", metadata);
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
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            
            // Tạo request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(paymentData, headers);
            
            // Gọi API PayOS để tạo thanh toán
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/payment-requests",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            // Xử lý response
            Map<String, Object> responseBody = response.getBody();
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
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo thanh toán đơn hàng: " + e.getMessage(), e);
        }
    }
    
    // Phương thức tạo chữ ký cho PayOS API v2
    private String generateSignature(String orderCode, String amount, String description, String returnUrl, String cancelUrl) {
        try {
            // Tạo chuỗi dữ liệu cần ký theo định dạng của PayOS
            // Format: amount=$amount&cancelUrl=$cancelUrl&description=$description&orderCode=$orderCode&returnUrl=$returnUrl
            String dataToSign = "amount=" + amount + 
                              "&cancelUrl=" + cancelUrl + 
                              "&description=" + description + 
                              "&orderCode=" + orderCode + 
                              "&returnUrl=" + returnUrl;
            
            System.out.println("Data to sign: " + dataToSign);
            
            // Tạo chữ ký bằng HMAC-SHA256
            javax.crypto.Mac sha256_HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secret_key = new javax.crypto.spec.SecretKeySpec(checksumKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            
            byte[] hash = sha256_HMAC.doFinal(dataToSign.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // Chuyển đổi byte array thành chuỗi hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = String.format("%02x", b); // Đảm bảo luôn có 2 ký tự hex cho mỗi byte
                hexString.append(hex);
            }
            
            System.out.println("Generated signature: " + hexString.toString());
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo chữ ký: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lấy thông tin chi tiết thanh toán từ PayOS API
     */
    private Map<String, Object> getPaymentInfoFromPayOS(String orderCode) {
        try {
            // Tạo header với thông tin xác thực
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            
            // Tạo request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            
            // Gọi API PayOS để lấy thông tin chi tiết
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/payment-requests/" + orderCode,
                    org.springframework.http.HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            
            // Trả về body của response
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi PayOS API: " + e.getMessage());
            return null;
        }
    }
    
    public Payment getPaymentStatus(Integer paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán với ID: " + paymentId));
        
        // Nếu thanh toán đang ở trạng thái PENDING, kiểm tra trạng thái từ PayOS
        if (payment.getStatus() == PaymentStatus.PENDING && payment.getPayosTransactionId() != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("x-client-id", clientId);
                headers.set("x-api-key", apiKey);
                
                HttpEntity<String> requestEntity = new HttpEntity<>(headers);
                ResponseEntity<Map> response = restTemplate.exchange(
                        apiUrl + "/payment-requests/" + payment.getPayosTransactionId(),
                        org.springframework.http.HttpMethod.GET,
                        requestEntity,
                        Map.class
                );
                
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    String status = (String) data.get("status");
                    
                    // Cập nhật trạng thái thanh toán
                    if ("PAID".equals(status)) {
                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setPaymentDate(LocalDateTime.now());
                    } else if ("CANCELLED".equals(status)) {
                        payment.setStatus(PaymentStatus.CANCELLED);
                    } else if ("FAILED".equals(status)) {
                        payment.setStatus(PaymentStatus.FAILED);
                    }
                    
                    paymentRepository.save(payment);
                }
            } catch (Exception e) {
                // Log lỗi nhưng không throw exception để vẫn trả về trạng thái hiện tại
                System.err.println("Lỗi khi kiểm tra trạng thái thanh toán từ PayOS: " + e.getMessage());
            }
        }
        
        return payment;
    }
    
    public PayOSVerifyResponse verifyPaymentReturn(Map<String, String> params) {
        try {
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
            
            // Tìm thanh toán trong database
            Optional<Payment> paymentOpt = paymentRepository.findByTransactionNo(orderCode);
            if (!paymentOpt.isPresent()) {
                return PayOSVerifyResponse.builder()
                        .success(false)
                        .message("Không tìm thấy giao dịch")
                        .build();
            }
            
            Payment payment = paymentOpt.get();
            
            // Kiểm tra trạng thái từ PayOS
            if ("PAID".equals(status)) {
                // Cập nhật trạng thái thanh toán thành COMPLETED
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPaymentDate(LocalDateTime.now());
                
                // Lưu payosTransactionId nếu có
                if (payosId != null) {
                    payment.setPayosTransactionId(payosId);
                }
                
                payment = paymentRepository.save(payment);
                
                // Xác thực giao dịch với PayOS API
                Map<String, Object> paymentInfo = getPaymentInfoFromPayOS(payosId != null ? payosId : payment.getPayosTransactionId());
                System.out.println("Phản hồi xác thực từ PayOS: " + paymentInfo);
                
                if (paymentInfo != null && paymentInfo.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) paymentInfo.get("data");
                    
                    // Lấy thông tin đặt lịch từ Payment
                    AppointmentDTO appointmentData = new AppointmentDTO();
                    
                    try {
                        // In ra toàn bộ dữ liệu để debug
                        System.out.println("Toàn bộ dữ liệu từ PayOS: " + data);
                        
                        // Đặt paymentId trước
                        appointmentData.setPaymentId(payment.getId());
                        
                        // Đặt doctorId nếu có (không bắt buộc)
                        appointmentData.setDoctorId(payment.getDoctorId());
                        System.out.println("doctorId: " + appointmentData.getDoctorId());
                        
                        // Đặt serviceId nếu có (không bắt buộc)
                        appointmentData.setServiceId(payment.getServiceId());
                        System.out.println("serviceId: " + appointmentData.getServiceId());
                        
                        // Đảm bảo appointmentDate đúng định dạng yyyy-MM-dd
                        if (payment.getAppointmentDate() == null) {
                            // Nếu không có ngày, sử dụng ngày mặc định (ngày mai)
                            appointmentData.setAppointmentDate(LocalDate.now().plusDays(1).toString());
                        } else {
                            // Đảm bảo đúng định dạng yyyy-MM-dd
                            appointmentData.setAppointmentDate(payment.getAppointmentDate());
                        }
                        System.out.println("appointmentDate: " + appointmentData.getAppointmentDate());
                        
                        // Chuyển đổi timeSlot từ số sang chuỗi định dạng HH:mm
                        if (payment.getTimeSlot() == null) {
                            // Nếu không có giờ, sử dụng giờ mặc định (8:00)
                            appointmentData.setTimeSlot("08:00");
                        } else {
                            // Chuyển đổi từ số sang chuỗi định dạng HH:mm
                            String timeSlotStr = String.valueOf(payment.getTimeSlot());
                            int hour;
                            try {
                                hour = Integer.parseInt(timeSlotStr);
                            } catch (NumberFormatException e) {
                                // Nếu không thể chuyển đổi, sử dụng giờ mặc định
                                hour = 8;
                                System.err.println("Lỗi chuyển đổi timeSlot: " + e.getMessage());
                            }
                            appointmentData.setTimeSlot(String.format("%02d:00", hour));
                        }
                        System.out.println("timeSlot: " + appointmentData.getTimeSlot());
                        
                        // Kiểm tra và đặt userId
                        if (payment.getUserId() == null) {
                            // Nếu không có userId, tìm kiếm người dùng dựa trên email
                            String patientEmail = payment.getPatientEmail();
                            
                            // Ném lỗi nếu không có email để xác định người dùng
                            if (patientEmail == null || patientEmail.isEmpty()) {
                                throw new IllegalStateException("Không có thông tin email để xác định người dùng");
                            }
                            
                            // Tìm người dùng dựa trên email
                            Optional<User> userOpt = userRepository.findByEmail(patientEmail);
                            if (userOpt.isPresent()) {
                                appointmentData.setUserId(userOpt.get().getId());
                            } else {
                                // Nếu không tìm thấy, đặt userId = 0 (người dùng không xác định)
                                appointmentData.setUserId(0);
                            }
                        } else {
                            appointmentData.setUserId(payment.getUserId());
                        }
                        System.out.println("userId: " + appointmentData.getUserId());
                        
                        // Đặt thông tin bệnh nhân
                        System.out.println("[UTF-8 DEBUG] verifyPaymentReturn - patientName from DB: " + payment.getPatientName());
                        System.out.println("[UTF-8 DEBUG] verifyPaymentReturn - notes from DB: " + payment.getNotes());
                        appointmentData.setPatientName(payment.getPatientName());
                        appointmentData.setPatientEmail(payment.getPatientEmail());
                        appointmentData.setPatientPhone(payment.getPatientPhone());
                        appointmentData.setNotes(payment.getNotes());
                        System.out.println("patientName: " + appointmentData.getPatientName());
                        System.out.println("patientEmail: " + appointmentData.getPatientEmail());
                        System.out.println("patientPhone: " + appointmentData.getPatientPhone());
                        System.out.println("notes: " + appointmentData.getNotes());
                        
                        // Đặt paymentId
                        System.out.println("paymentId: " + appointmentData.getPaymentId());
                        
                        // TẠO APPOINTMENT THỰC SỰ TRONG DATABASE
                        try {
                            System.out.println("Bắt đầu tạo appointment với dữ liệu: " + appointmentData);
                            org.eyespire.eyespireapi.model.Appointment createdAppointment = appointmentService.createAppointment(appointmentData);
                            System.out.println("Đã tạo appointment thành công với ID: " + createdAppointment.getId());
                            
                            // Cập nhật appointmentData với ID của appointment vừa tạo
                            appointmentData.setId(createdAppointment.getId());
                        } catch (Exception appointmentError) {
                            System.err.println("Lỗi khi tạo appointment: " + appointmentError.getMessage());
                            appointmentError.printStackTrace();
                            
                            // Vẫn trả về thành công thanh toán nhưng ghi nhận lỗi tạo appointment
                            return PayOSVerifyResponse.builder()
                                    .success(true)
                                    .message("Thanh toán thành công nhưng có lỗi khi tạo lịch hẹn: " + appointmentError.getMessage())
                                    .status(status)
                                    .paymentId(payment.getId())
                                    .transactionNo(orderCode)
                                    .amount(payment.getAmount())
                                    .paymentDate(LocalDateTime.now())
                                    .payosTransactionId(payosId)
                                    .appointmentData(appointmentData)
                                    .build();
                        }
                        
                        // Tạo response
                        return PayOSVerifyResponse.builder()
                                .success(true)
                                .message("Thanh toán và tạo lịch hẹn thành công")
                                .status(status) // Truyền trạng thái từ PayOS
                                .paymentId(payment.getId())
                                .transactionNo(orderCode)
                                .amount(payment.getAmount())
                                .paymentDate(LocalDateTime.now())
                                .payosTransactionId(payosId) // Lưu ID giao dịch PayOS
                                .appointmentData(appointmentData)
                                .build();
                        
                    } catch (Exception e) {
                        System.err.println("Lỗi khi xử lý dữ liệu thanh toán: " + e.getMessage());
                        e.printStackTrace();
                        
                        // Vẫn trả về thành công nếu có lỗi khi xử lý dữ liệu
                        return PayOSVerifyResponse.builder()
                                .success(true)
                                .message("Thanh toán thành công nhưng có lỗi khi xử lý dữ liệu")
                                .status(status) // Truyền trạng thái từ PayOS
                                .paymentId(payment.getId())
                                .transactionNo(orderCode)
                                .amount(payment.getAmount())
                                .paymentDate(LocalDateTime.now())
                                .build();
                    }
                }
            }
            
            // Trả về response mặc định nếu không phải trạng thái PAID
            return PayOSVerifyResponse.builder()
                    .success("PAID".equals(status))
                    .message("PAID".equals(status) ? "Thanh toán thành công" : "Thanh toán chưa hoàn tất")
                    .status(status) // Truyền trạng thái từ PayOS
                    .paymentId(payment.getId())
                    .transactionNo(orderCode)
                    .amount(payment.getAmount())
                    .paymentDate(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            System.err.println("Lỗi khi xác thực thanh toán: " + e.getMessage());
            e.printStackTrace();
            return PayOSVerifyResponse.builder()
                    .success(false)
                    .message("Lỗi khi xác thực thanh toán: " + e.getMessage())
                    .build();
        }
    }
    
    private long generateOrderCode() {
        // Tạo mã giao dịch dạng số dương không vượt quá 9007199254740991 (MAX_SAFE_INTEGER trong JavaScript)
        // Sử dụng timestamp hiện tại nhưng đảm bảo không vượt quá giới hạn
        long timestamp = System.currentTimeMillis() % 1000000000; // Lấy 9 chữ số cuối của timestamp
        return timestamp + new Random().nextInt(1000); // Thêm số ngẫu nhiên để tránh trùng lặp
    }
}
