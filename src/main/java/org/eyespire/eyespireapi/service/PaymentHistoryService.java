package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.PaymentHistoryDTO;
import org.eyespire.eyespireapi.model.*;
import org.eyespire.eyespireapi.model.enums.PaymentStatus;
import org.eyespire.eyespireapi.repository.AppointmentInvoiceRepository;
import org.eyespire.eyespireapi.repository.OrderPaymentRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentHistoryService {

    @Autowired
    private OrderPaymentRepository orderPaymentRepository;

    @Autowired
    private AppointmentInvoiceRepository appointmentInvoiceRepository;

    @Autowired
    private UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Lấy lịch sử thanh toán của người dùng
     * @param userId ID của người dùng
     * @return Danh sách lịch sử thanh toán
     */
    public List<PaymentHistoryDTO> getUserPaymentHistory(Integer userId) {
        List<PaymentHistoryDTO> paymentHistory = new ArrayList<>();

        // Lấy danh sách hóa đơn dịch vụ từ appointment_invoices (chỉ các appointment COMPLETED)
        List<AppointmentInvoice> appointmentInvoices = appointmentInvoiceRepository.findByPatientIdAndAppointmentCompleted(userId);
        for (AppointmentInvoice appointmentInvoice : appointmentInvoices) {
            PaymentHistoryDTO dto = convertAppointmentInvoiceToDTO(appointmentInvoice);
            paymentHistory.add(dto);
        }

        // Lấy danh sách hóa đơn đơn hàng
        List<OrderPayment> orderPayments = orderPaymentRepository.findByOrderPatientId(userId);
        for (OrderPayment orderPayment : orderPayments) {
            PaymentHistoryDTO dto = convertOrderPaymentToDTO(orderPayment);
            paymentHistory.add(dto);
        }

        // Sắp xếp theo ngày (mới nhất lên đầu)
        paymentHistory.sort(Comparator.comparing(PaymentHistoryDTO::getCreatedAt).reversed());

        return paymentHistory;
    }

    /**
     * Lấy chi tiết hóa đơn
     * @param id ID của hóa đơn
     * @param type Loại hóa đơn (service, order, hoặc invoice)
     * @return Chi tiết hóa đơn
     */
    public PaymentHistoryDTO getPaymentDetail(String id, String type) {
        if ("service".equals(type) || "invoice".equals(type)) {
            try {
                Integer invoiceId = Integer.parseInt(id.replace("INV", "").replace("SRV", ""));
                AppointmentInvoice appointmentInvoice = appointmentInvoiceRepository.findById(invoiceId).orElse(null);
                if (appointmentInvoice != null) {
                    return convertAppointmentInvoiceToDTO(appointmentInvoice);
                }
            } catch (NumberFormatException e) {
                // Xử lý lỗi chuyển đổi ID
            }
        } else if ("order".equals(type)) {
            try {
                Integer paymentId = Integer.parseInt(id.replace("ORD", ""));
                OrderPayment orderPayment = orderPaymentRepository.findById(paymentId).orElse(null);
                if (orderPayment != null) {
                    return convertOrderPaymentToDTO(orderPayment);
                }
            } catch (NumberFormatException e) {
                // Xử lý lỗi chuyển đổi ID
            }
        }
        return null;
    }

    /**
     * Lọc lịch sử thanh toán theo loại
     * @param userId ID của người dùng
     * @param type Loại hóa đơn (service hoặc order)
     * @return Danh sách lịch sử thanh toán đã lọc
     */
    public List<PaymentHistoryDTO> filterPaymentHistory(Integer userId, String type) {
        List<PaymentHistoryDTO> allPayments = getUserPaymentHistory(userId);
        
        if (type == null || type.isEmpty() || "all".equals(type)) {
            return allPayments;
        }
        
        return allPayments.stream()
                .filter(payment -> type.equals(payment.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Tìm kiếm lịch sử thanh toán
     * @param userId ID của người dùng
     * @param query Từ khóa tìm kiếm
     * @return Danh sách lịch sử thanh toán phù hợp với từ khóa
     */
    public List<PaymentHistoryDTO> searchPaymentHistory(Integer userId, String query) {
        List<PaymentHistoryDTO> allPayments = getUserPaymentHistory(userId);
        
        if (query == null || query.isEmpty()) {
            return allPayments;
        }
        
        String lowerQuery = query.toLowerCase();
        
        return allPayments.stream()
                .filter(payment -> 
                    (payment.getServiceName() != null && payment.getServiceName().toLowerCase().contains(lowerQuery)) ||
                    (payment.getStatus() != null && payment.getStatus().toLowerCase().contains(lowerQuery)) ||
                    (payment.getId() != null && payment.getId().toLowerCase().contains(lowerQuery))
                )
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi Payment thành PaymentHistoryDTO
     * @param payment Đối tượng Payment
     * @return PaymentHistoryDTO
     */
    private PaymentHistoryDTO convertServicePaymentToDTO(Payment payment) {
        PaymentHistoryDTO dto = new PaymentHistoryDTO();
        
        dto.setId("SRV" + payment.getId());
        
        if (payment.getPaymentDate() != null) {
            dto.setDate(payment.getPaymentDate().format(DATE_FORMATTER));
            dto.setPaymentTime(payment.getPaymentDate().format(TIME_FORMATTER));
        } else if (payment.getCreatedAt() != null) {
            dto.setDate(payment.getCreatedAt().format(DATE_FORMATTER));
            dto.setPaymentTime(payment.getCreatedAt().format(TIME_FORMATTER));
        }
        
        // Lấy tên dịch vụ từ appointment invoice nếu có
        if (payment.getAppointmentInvoice() != null && payment.getAppointmentInvoice().getAppointment() != null) {
            Appointment appointment = payment.getAppointmentInvoice().getAppointment();
            
            // Lấy tên dịch vụ từ danh sách dịch vụ của lịch hẹn
            if (appointment.getServices() != null && !appointment.getServices().isEmpty()) {
                dto.setServiceName(appointment.getServices().get(0).getName());
                if (appointment.getServices().size() > 1) {
                    dto.setServiceName(dto.getServiceName() + " và " + (appointment.getServices().size() - 1) + " dịch vụ khác");
                }
            } else {
                dto.setServiceName("Dịch vụ khám mắt");
            }
            
            // Thông tin bác sĩ
            if (appointment.getDoctor() != null) {
                dto.setDoctorName(appointment.getDoctor().getUser().getName());
            }
            
            // Thông tin lịch hẹn
            dto.setAppointmentId(appointment.getId());
            if (appointment.getAppointmentTime() != null) {
                dto.setAppointmentDate(appointment.getAppointmentTime().format(DATE_FORMATTER));
                dto.setTimeSlot(appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
        } else {
            dto.setServiceName("Dịch vụ khám mắt");
        }
        
        dto.setAmount(payment.getAmount().toString() + " đ");
        dto.setStatus(payment.getStatus().toString().toLowerCase());
        dto.setType("service");
        dto.setTransactionNo(payment.getTransactionNo());
        
        // Phương thức thanh toán
        if (payment.getPayosPaymentMethod() != null && !payment.getPayosPaymentMethod().isEmpty()) {
            dto.setPaymentMethod(payment.getPayosPaymentMethod());
        } else if (payment.getPayosBankCode() != null && !payment.getPayosBankCode().isEmpty()) {
            dto.setPaymentMethod("Chuyển khoản ngân hàng (" + payment.getPayosBankCode() + ")");
        } else {
            dto.setPaymentMethod("Tiền mặt");
        }
        
        // Thông tin người thanh toán
        dto.setPayerName(payment.getPatientName());
        dto.setPatientName(payment.getPatientName());
        dto.setPatientEmail(payment.getPatientEmail());
        dto.setPatientPhone(payment.getPatientPhone());
        
        // Ghi chú
        dto.setNotes(payment.getNotes());
        
        // Thời gian tạo
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        
        // Mặc định không mở rộng
        dto.setIsExpanded(false);
        
        return dto;
    }

    /**
     * Chuyển đổi OrderPayment thành PaymentHistoryDTO
     * @param orderPayment Đối tượng OrderPayment
     * @return PaymentHistoryDTO
     */
    private PaymentHistoryDTO convertOrderPaymentToDTO(OrderPayment orderPayment) {
        PaymentHistoryDTO dto = new PaymentHistoryDTO();
        
        dto.setId("ORD" + orderPayment.getId());
        
        if (orderPayment.getPaymentDate() != null) {
            dto.setDate(orderPayment.getPaymentDate().format(DATE_FORMATTER));
            dto.setPaymentTime(orderPayment.getPaymentDate().format(TIME_FORMATTER));
        } else if (orderPayment.getCreatedAt() != null) {
            dto.setDate(orderPayment.getCreatedAt().format(DATE_FORMATTER));
            dto.setPaymentTime(orderPayment.getCreatedAt().format(TIME_FORMATTER));
        }
        
        // Lấy thông tin đơn hàng
        if (orderPayment.getOrder() != null) {
            Order order = orderPayment.getOrder();
            
            // Lấy tên sản phẩm từ danh sách sản phẩm của đơn hàng
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                OrderItem firstItem = order.getOrderItems().get(0);
                String productName = firstItem.getProduct() != null ? firstItem.getProduct().getName() : "Sản phẩm";
                dto.setServiceName(productName);
                
                if (order.getOrderItems().size() > 1) {
                    dto.setServiceName(productName + " và " + (order.getOrderItems().size() - 1) + " sản phẩm khác");
                }
            } else {
                dto.setServiceName("Đơn hàng sản phẩm");
            }
            
            // Thông tin đơn hàng
            dto.setOrderId(order.getId());
            dto.setShippingAddress(order.getShippingAddress());
            
            // Thông tin người đặt hàng
            if (order.getPatient() != null) {
                dto.setPayerName(order.getPatient().getName());
                dto.setPatientName(order.getPatient().getName());
                dto.setPatientEmail(order.getPatient().getEmail());
                dto.setPatientPhone(order.getPatient().getPhone());
            }
        } else {
            dto.setServiceName("Đơn hàng sản phẩm");
        }
        
        dto.setAmount(orderPayment.getAmount().toString() + " đ");
        dto.setStatus(orderPayment.getStatus().toString().toLowerCase());
        dto.setType("order");
        dto.setTransactionNo(orderPayment.getTransactionNo());
        
        // Phương thức thanh toán (mặc định là tiền mặt vì không có thông tin chi tiết)
        dto.setPaymentMethod("Tiền mặt");
        
        // Thời gian tạo
        dto.setCreatedAt(orderPayment.getCreatedAt());
        dto.setUpdatedAt(orderPayment.getUpdatedAt());
        
        // Mặc định không mở rộng
        dto.setIsExpanded(false);
        
        return dto;
    }

    /**
     * Chuyển đổi AppointmentInvoice thành PaymentHistoryDTO
     * @param appointmentInvoice Đối tượng AppointmentInvoice
     * @return PaymentHistoryDTO
     */
    private PaymentHistoryDTO convertAppointmentInvoiceToDTO(AppointmentInvoice appointmentInvoice) {
        PaymentHistoryDTO dto = new PaymentHistoryDTO();
        
        dto.setId("INV" + appointmentInvoice.getId());
        
        // Sử dụng paidAt nếu có, nếu không thì dùng createdAt
        if (appointmentInvoice.getPaidAt() != null) {
            dto.setDate(appointmentInvoice.getPaidAt().format(DATE_FORMATTER));
            dto.setPaymentTime(appointmentInvoice.getPaidAt().format(TIME_FORMATTER));
        } else if (appointmentInvoice.getCreatedAt() != null) {
            dto.setDate(appointmentInvoice.getCreatedAt().format(DATE_FORMATTER));
            dto.setPaymentTime(appointmentInvoice.getCreatedAt().format(TIME_FORMATTER));
        }
        
        // Lấy thông tin từ appointment
        if (appointmentInvoice.getAppointment() != null) {
            Appointment appointment = appointmentInvoice.getAppointment();
            
            // Lấy tên dịch vụ từ danh sách dịch vụ của lịch hẹn
            if (appointment.getServices() != null && !appointment.getServices().isEmpty()) {
                dto.setServiceName(appointment.getServices().get(0).getName());
                if (appointment.getServices().size() > 1) {
                    dto.setServiceName(dto.getServiceName() + " và " + (appointment.getServices().size() - 1) + " dịch vụ khác");
                }
            } else {
                dto.setServiceName("Dịch vụ khám mắt");
            }
            
            // Thông tin bác sĩ
            if (appointment.getDoctor() != null && appointment.getDoctor().getUser() != null) {
                dto.setDoctorName(appointment.getDoctor().getUser().getName());
            }
            
            // Thông tin lịch hẹn
            dto.setAppointmentId(appointment.getId());
            if (appointment.getAppointmentTime() != null) {
                dto.setAppointmentDate(appointment.getAppointmentTime().format(DATE_FORMATTER));
                dto.setTimeSlot(appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            
            // Thông tin bệnh nhân từ appointment
            if (appointment.getPatient() != null) {
                dto.setPayerName(appointment.getPatient().getName());
                dto.setPatientName(appointment.getPatient().getName());
                dto.setPatientEmail(appointment.getPatient().getEmail());
                dto.setPatientPhone(appointment.getPatient().getPhone());
            }
        } else {
            dto.setServiceName("Dịch vụ khám mắt");
        }
        
        // Thông tin hóa đơn
        dto.setAmount(appointmentInvoice.getRemainingAmount().toString() + " đ");
        dto.setStatus(appointmentInvoice.getIsFullyPaid() ? "paid" : "pending");
        dto.setType("service");
        dto.setTransactionNo(appointmentInvoice.getTransactionId());
        
        // Phương thức thanh toán (mặc định vì AppointmentInvoice không có thông tin này)
        if (appointmentInvoice.getTransactionId() != null && !appointmentInvoice.getTransactionId().isEmpty()) {
            if (appointmentInvoice.getTransactionId().startsWith("CASH")) {
                dto.setPaymentMethod("Tiền mặt");
            } else {
                dto.setPaymentMethod("Chuyển khoản");
            }
        } else {
            dto.setPaymentMethod("Chưa thanh toán");
        }
        
        // Thời gian tạo
        dto.setCreatedAt(appointmentInvoice.getCreatedAt());
        dto.setUpdatedAt(appointmentInvoice.getCreatedAt()); // AppointmentInvoice không có updatedAt
        
        // Mặc định không mở rộng
        dto.setIsExpanded(false);
        
        return dto;
    }
}
