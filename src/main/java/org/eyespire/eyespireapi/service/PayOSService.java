package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.AppointmentDTO;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
                    payment.setPatientName(appointmentData.getPatientName());
                }
                
                if (appointmentData.getPatientEmail() != null) {
                    payment.setPatientEmail(appointmentData.getPatientEmail());
                }
                
                if (appointmentData.getPatientPhone() != null) {
                    payment.setPatientPhone(appointmentData.getPatientPhone());
                }
                
                if (appointmentData.getNotes() != null) {
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
                payment.getAmount().toString(), 
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
                payment = paymentRepository.save(payment);
                
                // Xác thực giao dịch với PayOS API
                Map<String, Object> paymentInfo = getPaymentInfoFromPayOS(payment.getPayosTransactionId());
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
                        
                        // Kiểm tra và đảm bảo doctorId không null
                        if (payment.getDoctorId() == null) {
                            throw new RuntimeException("Thiếu thông tin bác sĩ");
                        }
                        appointmentData.setDoctorId(payment.getDoctorId());
                        System.out.println("doctorId: " + appointmentData.getDoctorId());
                        
                        // Kiểm tra và đảm bảo serviceId không null
                        if (payment.getServiceId() == null) {
                            throw new RuntimeException("Thiếu thông tin dịch vụ");
                        }
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
                                throw new RuntimeException("Không thể xác định người dùng: thiếu thông tin email");
                            }
                            
                            // Tìm kiếm người dùng theo email
                            Integer userId = userRepository.findByEmail(patientEmail)
                                .map(User::getId)
                                .orElse(null);
                            
                            // Nếu không tìm thấy, ném lỗi
                            if (userId == null) {
                                throw new RuntimeException("Không tìm thấy người dùng với email: " + patientEmail);
                            }
                            
                            appointmentData.setUserId(userId);
                            System.out.println("Tìm thấy userId: " + userId + " từ email bệnh nhân");
                        } else {
                            appointmentData.setUserId(payment.getUserId());
                        }
                        System.out.println("userId: " + appointmentData.getUserId());
                        
                        appointmentData.setPatientName(payment.getPatientName());
                        System.out.println("patientName: " + appointmentData.getPatientName());
                        
                        appointmentData.setPatientEmail(payment.getPatientEmail());
                        System.out.println("patientEmail: " + appointmentData.getPatientEmail());
                        
                        appointmentData.setPatientPhone(payment.getPatientPhone());
                        System.out.println("patientPhone: " + appointmentData.getPatientPhone());
                        
                        appointmentData.setNotes(payment.getNotes());
                        System.out.println("notes: " + appointmentData.getNotes());
                        
                        System.out.println("paymentId: " + appointmentData.getPaymentId());
                        
                        return PayOSVerifyResponse.builder()
                                .success(true)
                                .message("Thanh toán thành công")
                                .paymentId(payment.getId())
                                .transactionNo(payment.getTransactionNo())
                                .amount(payment.getAmount())
                                .paymentDate(payment.getPaymentDate())
                                .appointmentData(appointmentData)
                                .build();
                    } catch (Exception e) {
                        System.err.println("Lỗi khi xử lý dữ liệu thanh toán: " + e.getMessage());
                        e.printStackTrace();
                        
                        // Trả về thông báo lỗi chi tiết hơn
                        return PayOSVerifyResponse.builder()
                                .success(false)
                                .message("Lỗi khi xử lý dữ liệu thanh toán: " + e.getMessage())
                                .paymentId(payment.getId())
                                .build();
                    }
                }
            }
            
            // Cập nhật trạng thái thanh toán thất bại
            if ("CANCELLED".equals(status)) {
                payment.setStatus(PaymentStatus.CANCELLED);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }
            paymentRepository.save(payment);
            
            return PayOSVerifyResponse.builder()
                    .success(false)
                    .message("Thanh toán không thành công với trạng thái: " + status)
                    .paymentId(payment.getId())
                    .build();
        } catch (Exception e) {
            e.printStackTrace(); // In ra stack trace để dễ debug
            return PayOSVerifyResponse.builder()
                    .success(false)
                    .message("Lỗi khi xác thực thanh toán: " + e.getMessage())
                    .build();
        }
    }
    
    private long generateOrderCode() {
        // Tạo mã giao dịch dạng số dương
        return System.currentTimeMillis(); // Sử dụng timestamp hiện tại làm mã giao dịch
    }
}
