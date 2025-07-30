package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.*;
import org.eyespire.eyespireapi.model.*;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.model.enums.PrescriptionStatus;
import org.eyespire.eyespireapi.service.AppointmentInvoiceService;
import org.eyespire.eyespireapi.service.AppointmentService;
import org.eyespire.eyespireapi.service.UserService;
import org.eyespire.eyespireapi.service.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "https://eyespire.vercel.app", allowCredentials = "true")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentInvoiceService appointmentInvoiceService;

    @Autowired
    private UserService userService;

    @Autowired
    private RefundService refundService;

    @PostMapping
    public ResponseEntity<?> bookAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        try {
            Appointment appointment = appointmentService.createAppointment(appointmentDTO);
            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể đặt lịch khám: " + e.getMessage());
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientAppointments(@PathVariable Integer patientId) {
        try {
            User patient = userService.getUserById(patientId);
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy bệnh nhân");
            }

            List<Appointment> appointments = appointmentService.getAppointmentsByPatient(patientId);
            // Convert to DTOs to include refund information
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(appointmentDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách lịch hẹn: " + e.getMessage());
        }
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorAppointments(@PathVariable Integer doctorId) {
        try {
            List<Appointment> appointments = appointmentService.getAppointmentsByDoctor(doctorId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách lịch hẹn: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAppointmentById(@PathVariable Integer id) {
        try {
            Optional<Appointment> appointment = appointmentService.getAppointmentById(id);
            return appointment.map(appt -> ResponseEntity.ok(convertToDTO(appt)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thông tin chi tiết cuộc hẹn: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updateAppointmentStatus(@PathVariable Integer id, @RequestBody Map<String, String> statusUpdate) {
        try {
            String statusStr = statusUpdate.get("status");
            if (statusStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thiếu thông tin trạng thái");
            }

            AppointmentStatus status;
            try {
                status = AppointmentStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trạng thái không hợp lệ");
            }

            Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(id);
            if (!appointmentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy lịch hẹn");
            }

            Optional<Appointment> updatedAppointment = appointmentService.updateAppointmentStatus(id, status);
            return updatedAppointment.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật trạng thái lịch hẹn: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAppointment(@PathVariable Integer id, @RequestBody AppointmentDTO appointmentDTO) {
        try {
            Optional<Appointment> updatedAppointment = appointmentService.updateAppointment(id, appointmentDTO);
            return updatedAppointment.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi khi cập nhật lịch hẹn: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/services")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updateAppointmentServices(@PathVariable Integer id, @RequestBody Map<String, List<Integer>> serviceUpdate) {
        try {
            List<Integer> serviceIds = serviceUpdate.get("serviceIds");
            if (serviceIds == null || serviceIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Danh sách ID dịch vụ không hợp lệ");
            }

            Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(id);
            if (!appointmentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy lịch hẹn");
            }

            Optional<Appointment> updatedAppointment = appointmentService.updateAppointmentServices(id, serviceIds);
            return updatedAppointment.map(appt -> ResponseEntity.ok(convertToDTO(appt)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật dịch vụ lịch hẹn: " + e.getMessage());
        }
    }

    @GetMapping("/available-by-date")
    public ResponseEntity<?> getAvailableTimeSlotsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<DoctorTimeSlotDTO> availableSlots = appointmentService.getAvailableTimeSlotsByDate(date);
            return ResponseEntity.ok(availableSlots);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy khung giờ trống theo ngày: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllAppointments() {
        try {
            List<AppointmentDTO> appointments = appointmentService.getAllAppointments().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            if (appointments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Không có lịch hẹn nào được tìm thấy");
            }

            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách lịch hẹn: " + e.getMessage());
        }
    }

    @GetMapping("/by-date")
    public ResponseEntity<?> getAppointmentsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Appointment> appointments = appointmentService.getAppointmentsByDate(date);
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(appointmentDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi lấy danh sách cuộc hẹn theo ngày: " + e.getMessage());
        }
    }

    @GetMapping("/by-doctor-and-date")
    public ResponseEntity<?> getAppointmentsByDoctorAndDate(
            @RequestParam Integer doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Appointment> appointments = appointmentService.getAppointmentsByDoctorAndDate(doctorId, date);
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(appointmentDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi lấy danh sách cuộc hẹn theo bác sĩ và ngày: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/waiting-payment")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> setAppointmentWaitingPayment(
            @PathVariable Integer id,
            @RequestBody WaitingPaymentRequestDTO request) {
        try {
            Optional<Appointment> updatedAppointment = appointmentService.setAppointmentWaitingPayment(id, request.getTotalAmount());
            return updatedAppointment.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi chuyển trạng thái cuộc hẹn sang chờ thanh toán: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/mark-as-paid")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> markAppointmentAsPaid(
            @PathVariable Integer id,
            @RequestBody InvoicePaymentRequestDTO request) {
        try {
            Optional<Appointment> updatedAppointment = appointmentService.markAppointmentAsPaid(id, request.getTransactionId());
            return updatedAppointment.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi đánh dấu cuộc hẹn đã thanh toán: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        try {
            String cancellationReason = request.get("cancellationReason");
            Optional<Appointment> updatedAppointment = appointmentService.cancelAppointment(id, cancellationReason);
            return updatedAppointment.map(appt -> ResponseEntity.ok(convertToDTO(appt)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi hủy cuộc hẹn: " + e.getMessage());
        }
    }

    @GetMapping("/waiting-payment")
    public ResponseEntity<?> getWaitingPaymentAppointments() {
        try {
            List<Appointment> appointments = appointmentService.getWaitingPaymentAppointments();
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách cuộc hẹn đang chờ thanh toán: " + e.getMessage());
        }
    }

    @GetMapping("/doctor/{doctorId}/waiting-payment")
    public ResponseEntity<?> getWaitingPaymentAppointmentsByDoctor(@PathVariable Integer doctorId) {
        try {
            List<Appointment> appointments = appointmentService.getWaitingPaymentAppointmentsByDoctor(doctorId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách cuộc hẹn đang chờ thanh toán của bác sĩ: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<?> getAppointmentInvoice(@PathVariable Integer id) {
        try {
            Optional<AppointmentInvoice> invoice = appointmentInvoiceService.getInvoiceByAppointmentId(id);
            return invoice.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy thông tin hóa đơn của cuộc hẹn: " + e.getMessage());
        }
    }

    // NEW: Create invoice for an appointment
    @PostMapping("/{id}/invoice")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> createAppointmentInvoice(@PathVariable Integer id, @RequestBody InvoiceCreationRequestDTO request) {
        try {
            AppointmentInvoice invoice = appointmentInvoiceService.createInvoice(id, request.getServiceIds(), request.getMedications());
            return ResponseEntity.ok(invoice);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể tạo hóa đơn: " + e.getMessage());
        }
    }

    // NEW: Update prescription status
    @PutMapping("/{id}/invoice/prescription-status")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    public ResponseEntity<?> updatePrescriptionStatus(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        try {
            String statusStr = request.get("prescriptionStatus");
            if (statusStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thiếu thông tin trạng thái đơn thuốc");
            }

            PrescriptionStatus status;
            try {
                status = PrescriptionStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trạng thái đơn thuốc không hợp lệ");
            }

            Optional<AppointmentInvoice> updatedInvoice = appointmentInvoiceService.updatePrescriptionStatus(id, status);
            return updatedInvoice.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật trạng thái đơn thuốc: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/update-invoice")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updateInvoiceAndSetWaitingPayment(
            @PathVariable Integer id,
            @RequestBody InvoiceCreationRequestDTO request) {
        try {
            // Kiểm tra sự tồn tại của lịch hẹn
            Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(id);
            if (!appointmentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy lịch hẹn");
            }

            // Kiểm tra trạng thái lịch hẹn
            Appointment appointment = appointmentOpt.get();
            if (appointment.getStatus() != AppointmentStatus.DOCTOR_FINISHED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Lịch hẹn phải ở trạng thái DOCTOR_FINISHED để cập nhật hóa đơn");
            }

            // Gọi service để tạo hoặc cập nhật hóa đơn
            AppointmentInvoice invoice = appointmentInvoiceService.updateOrCreateInvoice(
                    id,
                    request.getServiceIds(),
                    request.getIncludeMedications() ? request.getMedications() : null
            );

            // Cập nhật trạng thái lịch hẹn sang WAITING_PAYMENT
            Optional<Appointment> updatedAppointment = appointmentService.updateAppointmentStatus(id, AppointmentStatus.WAITING_PAYMENT);
            if (!updatedAppointment.isPresent()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Lỗi khi cập nhật trạng thái lịch hẹn sang WAITING_PAYMENT");
            }

            return ResponseEntity.ok(invoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tạo hoặc cập nhật hóa đơn: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/create-invoice")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> createInvoiceAndSetWaitingPayment(
            @PathVariable Integer id,
            @RequestBody InvoiceCreationRequestDTO request) {
        try {
            // Kiểm tra xem cuộc hẹn có tồn tại không
            Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(id);
            if (!appointmentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy lịch hẹn");
            }

            Appointment appointment = appointmentOpt.get();
            // CHANGED: Validate appointment status
            if (appointment.getStatus() != AppointmentStatus.DOCTOR_FINISHED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cuộc hẹn phải ở trạng thái DOCTOR_FINISHED để tạo hóa đơn");
            }
            // END CHANGE

            // Tạo hóa đơn với thông tin dịch vụ và thuốc (nếu có)
            AppointmentInvoice invoice = appointmentInvoiceService.createInvoice(
                    id,
                    request.getServiceIds(),
                    request.getIncludeMedications() ? request.getMedications() : null
            );

            // CHANGED: Explicitly set appointment status to WAITING_PAYMENT
            appointment.setStatus(AppointmentStatus.WAITING_PAYMENT);
            appointmentService.updateAppointmentStatus(id, AppointmentStatus.WAITING_PAYMENT);
            // END CHANGE

            // Cập nhật trạng thái đơn thuốc
            if (request.getIncludeMedications() && request.getMedications() != null && !request.getMedications().isEmpty()) {
                appointmentInvoiceService.updatePrescriptionStatus(id, PrescriptionStatus.PENDING);
            } else {
                appointmentInvoiceService.updatePrescriptionStatus(id, PrescriptionStatus.NOT_BUY);
            }

            return ResponseEntity.ok(convertToDTO(appointment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi tạo hóa đơn và chuyển trạng thái: " + e.getMessage());
        }
    }


    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();

        dto.setId(appointment.getId());

        dto.setUserId(appointment.getPatient() != null ? appointment.getPatient().getId() : null);

        dto.setDoctorId(appointment.getDoctor() != null ? appointment.getDoctor().getId() : null);
        dto.setServiceIds(appointment.getServices() != null
                ? appointment.getServices().stream().map(MedicalService::getId).collect(Collectors.toList())
                : null);

        if (appointment.getAppointmentTime() != null) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            dto.setAppointmentDate(appointment.getAppointmentTime().format(dateFormatter));
            dto.setTimeSlot(appointment.getAppointmentTime().format(timeFormatter));
        } else {
            dto.setAppointmentDate(null);
            dto.setTimeSlot(null);
        }

        dto.setPatientName(appointment.getPatientName());
        dto.setPatientEmail(appointment.getPatientEmail());
        dto.setPatientPhone(appointment.getPatientPhone());
        dto.setNotes(appointment.getNotes());
        dto.setStatus(appointment.getStatus() != null ? appointment.getStatus().name() : null);
        dto.setCancellationReason(appointment.getCancellationReason());

        if (appointment.getPatient() != null) {
            UserDTO patientDTO = new UserDTO();
            patientDTO.setId(appointment.getPatient().getId());
            patientDTO.setName(appointment.getPatient().getName());
            patientDTO.setEmail(appointment.getPatient().getEmail());
            patientDTO.setPhone(appointment.getPatient().getPhone());
            patientDTO.setProvince(appointment.getPatient().getProvince());
            patientDTO.setDistrict(appointment.getPatient().getDistrict());
            patientDTO.setWard(appointment.getPatient().getWard());
            patientDTO.setAddressDetail(appointment.getPatient().getAddressDetail());
            patientDTO.setVillage(appointment.getPatient().getAddressDetail());
            patientDTO.setGender(appointment.getPatient().getGender() != null
                    ? appointment.getPatient().getGender().toString()
                    : null);
            patientDTO.setDateOfBirth(appointment.getPatient().getDateOfBirth() != null
                    ? appointment.getPatient().getDateOfBirth().toString()
                    : null);
            dto.setPatient(patientDTO);
        }

        try {
            Optional<AppointmentInvoice> invoiceOpt = appointmentInvoiceService.getInvoiceByAppointmentId(appointment.getId());
            if (invoiceOpt.isPresent()) {
                AppointmentInvoice invoice = invoiceOpt.get();
                dto.setTotalAmount(invoice.getTotalAmount());
                dto.setDepositAmount(invoice.getDepositAmount());
                dto.setRemainingAmount(invoice.getRemainingAmount());
                dto.setIsFullyPaid(invoice.getIsFullyPaid());
                dto.setPaidAt(invoice.getPaidAt());
                try {
                    if (invoice.getTransactionId() != null && !invoice.getTransactionId().isEmpty()) {
                        dto.setPaymentId(Integer.valueOf(invoice.getTransactionId()));
                    }
                } catch (NumberFormatException e) {
                    dto.setPaymentId(null);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông tin hóa đơn: " + e.getMessage());
        }

        if (appointment.getServices() != null && !appointment.getServices().isEmpty()) {
            dto.setServices(appointment.getServices());
        }

        if (appointment.getDoctor() != null) {
            DoctorDTO doctorDTO = new DoctorDTO();
            doctorDTO.setId(appointment.getDoctor().getId());
            doctorDTO.setName(appointment.getDoctor().getName());
            doctorDTO.setSpecialization(appointment.getDoctor().getSpecialization());
            doctorDTO.setImageUrl(appointment.getDoctor().getImageUrl());
            dto.setDoctor(doctorDTO);
        }
        
        // Thêm thông tin refund nếu appointment đã bị hủy
        try {
            if (appointment.getStatus() == AppointmentStatus.CANCELED) {
                List<Refund> refunds = refundService.getRefundByAppointmentId(appointment.getId());
                if (!refunds.isEmpty()) {
                    // Lấy refund mới nhất (đã sort theo createdAt desc)
                    Refund latestRefund = refunds.get(0);
                    dto.setRequiresManualRefund(true);
                    dto.setRefundStatus(latestRefund.getRefundStatus().name());
                    dto.setRefundAmount(latestRefund.getRefundAmount());
                    dto.setRefundCompletedBy(latestRefund.getRefundCompletedBy());
                    dto.setRefundCompletedByRole(latestRefund.getRefundCompletedByRole() != null ? latestRefund.getRefundCompletedByRole().name() : null);
                    dto.setRefundCompletedAt(latestRefund.getRefundCompletedAt());
                } else {
                    // Appointment đã hủy nhưng chưa có refund record
                    dto.setRequiresManualRefund(true);
                    dto.setRefundStatus("UNKNOWN");
                    dto.setRefundAmount(java.math.BigDecimal.valueOf(10000)); // Default amount
                }
            } else {
                // Appointment chưa hủy
                dto.setRequiresManualRefund(false);
                dto.setRefundStatus(null);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông tin refund: " + e.getMessage());
            e.printStackTrace();
            // Set default values nếu có lỗi
            dto.setRequiresManualRefund(false);
            dto.setRefundStatus(null);
        }

        return dto;
    }
}