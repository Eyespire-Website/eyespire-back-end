package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.ServiceFeedbackDTO;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.ServiceFeedback;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.repository.AppointmentRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.eyespire.eyespireapi.repository.ServiceFeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional
public class ServiceFeedbackService {

    @Autowired
    private ServiceFeedbackRepository serviceFeedbackRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Tạo feedback mới cho dịch vụ
     */
    public ServiceFeedback createFeedback(ServiceFeedbackDTO feedbackDTO) {
        // Validate dữ liệu đầu vào
        validateFeedbackDTO(feedbackDTO);

        // Kiểm tra appointment tồn tại
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(feedbackDTO.getAppointmentId());
        if (!appointmentOpt.isPresent()) {
            throw new RuntimeException("Không tìm thấy cuộc hẹn với ID: " + feedbackDTO.getAppointmentId());
        }

        Appointment appointment = appointmentOpt.get();
        if (!AppointmentStatus.COMPLETED.equals(appointment.getStatus())) {
            throw new RuntimeException("Chỉ có thể đánh giá các cuộc hẹn đã hoàn thành");
        }

        // Kiểm tra user tồn tại
        Optional<User> userOpt = userRepository.findById(feedbackDTO.getPatientId());
        if (!userOpt.isPresent()) {
            throw new RuntimeException("Không tìm thấy user với ID: " + feedbackDTO.getPatientId());
        }

        // Kiểm tra xem đã có feedback cho appointment này chưa
        Optional<ServiceFeedback> existingFeedback = serviceFeedbackRepository.findByAppointmentId(feedbackDTO.getAppointmentId());
        if (existingFeedback.isPresent()) {
            throw new RuntimeException("Cuộc hẹn này đã được đánh giá");
        }

        // Tạo feedback mới
        ServiceFeedback feedback = new ServiceFeedback();
        feedback.setAppointment(appointment);
        feedback.setPatient(userOpt.get());
        feedback.setRating(feedbackDTO.getRating());
        feedback.setComment(feedbackDTO.getComment());
        feedback.setCreatedAt(LocalDateTime.now());

        return serviceFeedbackRepository.save(feedback);
    }

    /**
     * Lấy feedback theo appointment ID
     */
    public ServiceFeedback getFeedbackByAppointmentId(Integer appointmentId) {
        return serviceFeedbackRepository.findByAppointmentId(appointmentId).orElse(null);
    }

    /**
     * Lấy danh sách feedback theo patient ID
     */
    public List<ServiceFeedback> getFeedbacksByPatientId(Integer patientId) {
        return serviceFeedbackRepository.findByPatientId(patientId);
    }

    /**
     * Cập nhật feedback
     */
    public ServiceFeedback updateFeedback(Integer feedbackId, ServiceFeedbackDTO feedbackDTO) {
        // Validate dữ liệu đầu vào (chỉ validate rating và comment cho update)
        if (feedbackDTO.getRating() != null && (feedbackDTO.getRating() < 1 || feedbackDTO.getRating() > 5)) {
            throw new RuntimeException("Rating phải từ 1 đến 5 sao");
        }
        
        if (feedbackDTO.getComment() != null && feedbackDTO.getComment().length() > 1000) {
            throw new RuntimeException("Comment không được vượt quá 1000 ký tự");
        }
        
        Optional<ServiceFeedback> feedbackOpt = serviceFeedbackRepository.findById(feedbackId);
        if (!feedbackOpt.isPresent()) {
            throw new RuntimeException("Feedback not found with ID: " + feedbackId);
        }

        ServiceFeedback feedback = feedbackOpt.get();

        // Cập nhật thông tin
        feedback.setRating(feedbackDTO.getRating());
        feedback.setComment(feedbackDTO.getComment());

        return serviceFeedbackRepository.save(feedback);
    }

    /**
     * Xóa feedback
     */
    public void deleteFeedback(Integer feedbackId) {
        Optional<ServiceFeedback> feedbackOpt = serviceFeedbackRepository.findById(feedbackId);
        if (!feedbackOpt.isPresent()) {
            throw new RuntimeException("Feedback not found with ID: " + feedbackId);
        }

        serviceFeedbackRepository.deleteById(feedbackId);
    }

    /**
     * Lấy tất cả feedback
     */
    public List<ServiceFeedback> getAllFeedbacks() {
        return serviceFeedbackRepository.findAll();
    }

    /**
     * Kiểm tra feedback có tồn tại cho appointment không
     */
    public boolean feedbackExistsForAppointment(Integer appointmentId) {
        return serviceFeedbackRepository.findByAppointmentId(appointmentId).isPresent();
    }

    /**
     * Lấy thống kê rating
     */
    public Map<String, Object> getRatingStatistics() {
        List<ServiceFeedback> allFeedbacks = serviceFeedbackRepository.findAll();
        
        Map<String, Object> statistics = new HashMap<>();
        
        if (allFeedbacks.isEmpty()) {
            statistics.put("totalFeedbacks", 0);
            statistics.put("averageRating", 0.0);
            statistics.put("ratingDistribution", new HashMap<Integer, Integer>());
            return statistics;
        }

        // Tính tổng số feedback
        statistics.put("totalFeedbacks", allFeedbacks.size());

        // Tính rating trung bình
        double averageRating = allFeedbacks.stream()
                .mapToInt(ServiceFeedback::getRating)
                .average()
                .orElse(0.0);
        statistics.put("averageRating", Math.round(averageRating * 100.0) / 100.0);

        // Phân bố rating
        Map<Integer, Integer> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0);
        }
        
        for (ServiceFeedback feedback : allFeedbacks) {
            int rating = feedback.getRating();
            ratingDistribution.put(rating, ratingDistribution.get(rating) + 1);
        }
        
        statistics.put("ratingDistribution", ratingDistribution);

        return statistics;
    }

    /**
     * Lấy feedback mới nhất
     */
    public List<ServiceFeedback> getRecentFeedbacks(int limit) {
        return serviceFeedbackRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * Lấy feedback theo rating
     */
    public List<ServiceFeedback> getFeedbacksByRating(Integer rating) {
        return serviceFeedbackRepository.findByRating(rating);
    }
    
    /**
     * Validate dữ liệu feedback
     */
    private void validateFeedbackDTO(ServiceFeedbackDTO feedbackDTO) {
        if (feedbackDTO == null) {
            throw new RuntimeException("Feedback data không được để trống");
        }
        
        if (feedbackDTO.getAppointmentId() == null) {
            throw new RuntimeException("Appointment ID không được để trống");
        }
        
        if (feedbackDTO.getPatientId() == null) {
            throw new RuntimeException("Patient ID không được để trống");
        }
        
        if (feedbackDTO.getRating() == null) {
            throw new RuntimeException("Rating không được để trống");
        }
        
        if (feedbackDTO.getRating() < 1 || feedbackDTO.getRating() > 5) {
            throw new RuntimeException("Rating phải từ 1 đến 5 sao");
        }
        
        if (feedbackDTO.getComment() != null && feedbackDTO.getComment().length() > 1000) {
            throw new RuntimeException("Comment không được vượt quá 1000 ký tự");
        }
    }
}