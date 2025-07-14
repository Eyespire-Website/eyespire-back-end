package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.ServiceFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceFeedbackRepository extends JpaRepository<ServiceFeedback, Integer> {

    /**
     * Tìm feedback theo appointment ID
     */
    @Query("SELECT sf FROM ServiceFeedback sf WHERE sf.appointment.id = :appointmentId")
    Optional<ServiceFeedback> findByAppointmentId(@Param("appointmentId") Integer appointmentId);

    /**
     * Tìm tất cả feedback của một patient
     */
    @Query("SELECT sf FROM ServiceFeedback sf WHERE sf.patient.id = :patientId ORDER BY sf.createdAt DESC")
    List<ServiceFeedback> findByPatientId(@Param("patientId") Integer patientId);

    /**
     * Tìm feedback theo rating
     */
    List<ServiceFeedback> findByRating(Integer rating);

    /**
     * Tìm 10 feedback mới nhất
     */
    List<ServiceFeedback> findTop10ByOrderByCreatedAtDesc();

    /**
     * Tìm feedback theo khoảng rating
     */
    @Query("SELECT sf FROM ServiceFeedback sf WHERE sf.rating BETWEEN :minRating AND :maxRating ORDER BY sf.createdAt DESC")
    List<ServiceFeedback> findByRatingBetween(@Param("minRating") Integer minRating, @Param("maxRating") Integer maxRating);

    /**
     * Đếm số feedback theo rating
     */
    @Query("SELECT COUNT(sf) FROM ServiceFeedback sf WHERE sf.rating = :rating")
    Long countByRating(@Param("rating") Integer rating);

    /**
     * Tính rating trung bình
     */
    @Query("SELECT AVG(sf.rating) FROM ServiceFeedback sf")
    Double getAverageRating();

    /**
     * Tìm feedback có comment
     */
    @Query("SELECT sf FROM ServiceFeedback sf WHERE sf.comment IS NOT NULL AND sf.comment != '' ORDER BY sf.createdAt DESC")
    List<ServiceFeedback> findFeedbacksWithComments();

    /**
     * Tìm feedback theo doctor (thông qua appointment)
     */
    @Query("SELECT sf FROM ServiceFeedback sf WHERE sf.appointment.doctor.id = :doctorId ORDER BY sf.createdAt DESC")
    List<ServiceFeedback> findByDoctorId(@Param("doctorId") Integer doctorId);

    /**
     * Tìm feedback theo service (thông qua appointment)
     */
    @Query("SELECT sf FROM ServiceFeedback sf JOIN sf.appointment a JOIN a.services s WHERE s.id = :serviceId ORDER BY sf.createdAt DESC")
    List<ServiceFeedback> findByServiceId(@Param("serviceId") Integer serviceId);

    /**
     * Kiểm tra xem patient đã feedback cho appointment chưa
     */
    @Query("SELECT COUNT(sf) > 0 FROM ServiceFeedback sf WHERE sf.appointment.id = :appointmentId AND sf.patient.id = :patientId")
    boolean existsByAppointmentIdAndPatientId(@Param("appointmentId") Integer appointmentId, @Param("patientId") Integer patientId);

    /**
     * Lấy thống kê rating cho doctor
     */
    @Query("SELECT sf.rating, COUNT(sf) FROM ServiceFeedback sf WHERE sf.appointment.doctor.id = :doctorId GROUP BY sf.rating")
    List<Object[]> getRatingStatisticsByDoctorId(@Param("doctorId") Integer doctorId);

    /**
     * Lấy thống kê rating cho service
     */
    @Query("SELECT sf.rating, COUNT(sf) FROM ServiceFeedback sf JOIN sf.appointment a JOIN a.services s WHERE s.id = :serviceId GROUP BY sf.rating")
    List<Object[]> getRatingStatisticsByServiceId(@Param("serviceId") Integer serviceId);
}