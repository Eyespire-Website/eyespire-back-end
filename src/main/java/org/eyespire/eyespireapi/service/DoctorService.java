package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.DoctorDTO;
import org.eyespire.eyespireapi.dto.DoctorTimeSlotDTO;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.model.DoctorAvailability;
import org.eyespire.eyespireapi.model.Specialty;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.model.enums.AvailabilityStatus;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.eyespire.eyespireapi.repository.AppointmentRepository;
import org.eyespire.eyespireapi.repository.DoctorAvailabilityRepository;
import org.eyespire.eyespireapi.repository.DoctorRepository;
import org.eyespire.eyespireapi.repository.SpecialtyRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorAvailabilityRepository doctorAvailabilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    /**
     * Lấy danh sách tất cả bác sĩ
     */
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    /**
     * Lấy thông tin bác sĩ theo ID
     */
    public Optional<Doctor> getDoctorById(Integer id) {
        return doctorRepository.findById(id);
    }

    /**
     * Lấy thông tin bác sĩ theo User ID
     */
    public Optional<Doctor> getDoctorByUserId(Integer userId) {
        return doctorRepository.findByUserId(userId);
    }

    /**
     * Lấy danh sách bác sĩ theo chuyên khoa
     */
    public List<Doctor> getDoctorsBySpecialty(Integer specialtyId) {
        return doctorRepository.findBySpecialtyId(specialtyId);
    }

    /**
     * Tạo mới thông tin bác sĩ liên kết với User
     */
    public Doctor createDoctor(DoctorDTO doctorDTO) {
        // Kiểm tra User có tồn tại không
        Optional<User> userOpt = userRepository.findById(doctorDTO.getUserId());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User không tồn tại");
        }

        User user = userOpt.get();

        // Kiểm tra User có phải là bác sĩ không
        if (user.getRole() != UserRole.DOCTOR) {
            throw new IllegalArgumentException("User không phải là bác sĩ");
        }

        // Kiểm tra Doctor đã tồn tại với userId này chưa
        Optional<Doctor> existingDoctor = doctorRepository.findByUserId(doctorDTO.getUserId());
        if (existingDoctor.isPresent()) {
            throw new IllegalArgumentException("Bác sĩ đã tồn tại với User ID này");
        }

        // Tạo mới Doctor
        Doctor doctor = new Doctor();
        doctor.setName(doctorDTO.getName() != null ? doctorDTO.getName() : user.getName());
        doctor.setQualification(doctorDTO.getQualification());
        doctor.setExperience(doctorDTO.getExperience());
        doctor.setImageUrl(doctorDTO.getImageUrl());
        doctor.setDescription(doctorDTO.getDescription());
        doctor.setUserId(doctorDTO.getUserId());

        // Thiết lập Specialty nếu có
        if (doctorDTO.getSpecialtyId() != null) {
            Optional<Specialty> specialtyOpt = specialtyRepository.findById(doctorDTO.getSpecialtyId());
            if (specialtyOpt.isPresent()) {
                Specialty specialty = specialtyOpt.get();
                doctor.setSpecialty(specialty);
                // Tự động cập nhật trường specialization từ tên của specialty
                doctor.setSpecialization(specialty.getName());
            }
        } else if (doctorDTO.getSpecialization() != null) {
            // Nếu không có specialtyId nhưng có specialization
            doctor.setSpecialization(doctorDTO.getSpecialization());
        }

        return doctorRepository.save(doctor);
    }

    /**
     * Cập nhật thông tin bác sĩ
     */
    public Doctor updateDoctor(Integer id, DoctorDTO doctorDTO) {
        // Kiểm tra Doctor có tồn tại không
        Optional<Doctor> doctorOpt = doctorRepository.findById(id);
        if (doctorOpt.isEmpty()) {
            throw new IllegalArgumentException("Bác sĩ không tồn tại");
        }

        Doctor doctor = doctorOpt.get();

        // Cập nhật thông tin
        if (doctorDTO.getName() != null) {
            doctor.setName(doctorDTO.getName());
        }

        if (doctorDTO.getQualification() != null) {
            doctor.setQualification(doctorDTO.getQualification());
        }

        if (doctorDTO.getExperience() != null) {
            doctor.setExperience(doctorDTO.getExperience());
        }

        if (doctorDTO.getImageUrl() != null) {
            doctor.setImageUrl(doctorDTO.getImageUrl());
        }

        if (doctorDTO.getDescription() != null) {
            doctor.setDescription(doctorDTO.getDescription());
        }

        // Cập nhật Specialty nếu có
        if (doctorDTO.getSpecialtyId() != null) {
            Optional<Specialty> specialtyOpt = specialtyRepository.findById(doctorDTO.getSpecialtyId());
            if (specialtyOpt.isPresent()) {
                Specialty specialty = specialtyOpt.get();
                doctor.setSpecialty(specialty);
                // Tự động cập nhật trường specialization từ tên của specialty
                doctor.setSpecialization(specialty.getName());
            }
        } else if (doctorDTO.getSpecialization() != null) {
            // Nếu không có specialtyId nhưng có specialization
            doctor.setSpecialization(doctorDTO.getSpecialization());
        }

        return doctorRepository.save(doctor);
    }
    /**
     * Lấy danh sách khung giờ trống của bác sĩ theo ngày
     */
    public List<DoctorTimeSlotDTO> getAvailableTimeSlots(Integer doctorId, LocalDate date) {
        return getAvailableTimeSlots(doctorId, date, null);
    }

    /**
     * Lấy danh sách khung giờ trống của bác sĩ theo ngày (có thể loại trừ appointment cụ thể)
     */
    public List<DoctorTimeSlotDTO> getAvailableTimeSlots(Integer doctorId, LocalDate date, Integer excludeAppointmentId) {
        // Tạo danh sách khung giờ mặc định từ 8h đến 16h
        List<DoctorTimeSlotDTO> timeSlots = createDefaultTimeSlots();

        // Mặc định tất cả các khung giờ là UNAVAILABLE và set doctorId
        for (DoctorTimeSlotDTO slot : timeSlots) {
            slot.setStatus(AvailabilityStatus.UNAVAILABLE);
            slot.setDoctorId(doctorId);
        }

        // Lấy danh sách khả năng của bác sĩ trong ngày từ database (nếu có)
        List<DoctorAvailability> availabilities = doctorAvailabilityRepository.findByDoctorIdAndDate(doctorId, date);

        // Cập nhật trạng thái khung giờ dựa trên khả năng của bác sĩ
        for (DoctorAvailability availability : availabilities) {
            if (availability.getStatus() == AvailabilityStatus.AVAILABLE) {
                LocalTime startTime = availability.getStartTime();
                LocalTime endTime = availability.getEndTime();

                // Cập nhật tất cả các khung giờ nằm trong khoảng thời gian làm việc
                for (DoctorTimeSlotDTO slot : timeSlots) {
                    LocalTime slotStartTime = slot.getStartTime();
                    LocalTime slotEndTime = slot.getEndTime();

                    // Nếu khung giờ nằm trong khoảng thời gian làm việc
                    if (!slotStartTime.isBefore(startTime) && !slotEndTime.isAfter(endTime)) {
                        slot.setStatus(AvailabilityStatus.AVAILABLE);
                    }
                }
            }
        }

        // Lấy các lịch hẹn PENDING và CONFIRMED của bác sĩ cụ thể này
        List<Appointment> doctorAppointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                        doctorId,
                        date.atStartOfDay(),
                        date.atTime(23, 59, 59)
                ).stream()
                .filter(appointment -> {
                    AppointmentStatus status = appointment.getStatus();
                    boolean isActiveStatus = status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED;
                    boolean isNotExcluded = excludeAppointmentId == null || !appointment.getId().equals(excludeAppointmentId);
                    return isActiveStatus && isNotExcluded;
                })
                .collect(Collectors.toList());

        // Lấy tất cả appointments trong ngày để check global constraint
        List<Appointment> allAppointments = appointmentRepository.findByAppointmentTimeBetween(
                        date.atStartOfDay(),
                        date.atTime(23, 59, 59)
                ).stream()
                .filter(appointment -> {
                    AppointmentStatus status = appointment.getStatus();
                    boolean isActiveStatus = status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED;
                    boolean isNotExcluded = excludeAppointmentId == null || !appointment.getId().equals(excludeAppointmentId);
                    return isActiveStatus && isNotExcluded;
                })
                .collect(Collectors.toList());

        // Đếm tổng số bác sĩ trong hệ thống (simplified logic)
        long totalAvailableDoctors = doctorRepository.count();

        // Cập nhật trạng thái slot
        for (DoctorTimeSlotDTO slot : timeSlots) {
            LocalTime slotTime = slot.getStartTime();

            // Check nếu bác sĩ này có appointment ở slot này
            boolean doctorHasAppointment = doctorAppointments.stream()
                    .anyMatch(appointment -> appointment.getAppointmentTime().toLocalTime().equals(slotTime));

            if (doctorHasAppointment) {
                // Bác sĩ này có appointment -> BOOKED
                slot.setStatus(AvailabilityStatus.BOOKED);
                slot.setAvailableCount(0);
            } else {
                // Bác sĩ này không có appointment, check global constraint
                long appointmentsAtThisSlot = allAppointments.stream()
                        .filter(appointment -> appointment.getAppointmentTime().toLocalTime().equals(slotTime))
                        .count();

                if (appointmentsAtThisSlot >= totalAvailableDoctors) {
                    // Slot đã full globally -> BOOKED
                    slot.setStatus(AvailabilityStatus.BOOKED);
                    slot.setAvailableCount(0);
                } else {
                    // CHỈ set AVAILABLE nếu slot đã được mark là AVAILABLE từ working hours
                    // Không được ghi đè UNAVAILABLE slots (bác sĩ không làm việc)
                    if (slot.getStatus() == AvailabilityStatus.AVAILABLE) {
                        // Slot còn chỗ và bác sĩ có working hours -> keep AVAILABLE
                        slot.setAvailableCount(1);
                    } else {
                        // Slot không trong working hours -> keep UNAVAILABLE
                        slot.setAvailableCount(0);
                    }
                }
            }
        }

        return timeSlots;
    }

    /**
     * Tạo danh sách khung giờ mặc định từ 8h đến 16h
     */
    private List<DoctorTimeSlotDTO> createDefaultTimeSlots() {
        List<DoctorTimeSlotDTO> timeSlots = new ArrayList<>();

        // Tạo các khung giờ từ 8h đến 16h, mỗi khung giờ cách nhau 1 tiếng
        for (int hour = 8; hour <= 16; hour++) {
            LocalTime startTime = LocalTime.of(hour, 0);
            LocalTime endTime = LocalTime.of(hour + 1, 0);

            DoctorTimeSlotDTO slot = new DoctorTimeSlotDTO(startTime, endTime, AvailabilityStatus.AVAILABLE);
            timeSlots.add(slot);
        }

        return timeSlots;
    }


    /**
     * Kiểm tra bác sĩ có khả dụng trong khung giờ đặt lịch không
     */
    public boolean isDoctorAvailable(Integer doctorId, LocalDateTime appointmentTime) {
        return isDoctorAvailable(doctorId, appointmentTime, null);
    }

    /**
     * Kiểm tra bác sĩ có khả dụng trong khung giờ đặt lịch không (có thể loại trừ appointment cụ thể)
     */
    public boolean isDoctorAvailable(Integer doctorId, LocalDateTime appointmentTime, Integer excludeAppointmentId) {
        LocalDate appointmentDate = appointmentTime.toLocalDate();
        LocalTime appointmentTimeOfDay = appointmentTime.toLocalTime();

        System.out.println("[DEBUG] Checking availability for doctorId: " + doctorId + ", date: " + appointmentDate + ", time: " + appointmentTimeOfDay);

        // Kiểm tra xem bác sĩ có lịch làm việc trong ngày và khung giờ đó không
        List<DoctorAvailability> availabilities = doctorAvailabilityRepository.findByDoctorIdAndDate(doctorId, appointmentDate);
        System.out.println("[DEBUG] Found " + availabilities.size() + " availability records");

        boolean isInWorkingHours = false;
        for (DoctorAvailability availability : availabilities) {
            System.out.println("[DEBUG] Availability: " + availability.getStartTime() + "-" + availability.getEndTime() + ", status: " + availability.getStatus());
            if (availability.getStatus() == AvailabilityStatus.AVAILABLE &&
                    !appointmentTimeOfDay.isBefore(availability.getStartTime()) &&
                    !appointmentTimeOfDay.isAfter(availability.getEndTime())) {
                isInWorkingHours = true;
                break;
            }
        }

        System.out.println("[DEBUG] Is in working hours: " + isInWorkingHours);

        // Nếu không nằm trong giờ làm việc, trả về false
        if (!isInWorkingHours) {
            return false;
        }

        // Kiểm tra xem bác sĩ đã có lịch hẹn trong khung giờ đó chưa
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndAppointmentTime(doctorId, appointmentTime);
        System.out.println("[DEBUG] Found " + appointments.size() + " appointments at this time");

        // Lọc chỉ lấy các cuộc hẹn đang chờ xác nhận (PENDING) hoặc đã xác nhận (CONFIRMED)
        // và loại trừ appointment hiện tại nếu có
        List<Appointment> activeAppointments = appointments.stream()
                .filter(appointment -> {
                    AppointmentStatus status = appointment.getStatus();
                    boolean isActiveStatus = status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED;
                    boolean isNotExcluded = excludeAppointmentId == null || !appointment.getId().equals(excludeAppointmentId);
                    System.out.println("[DEBUG] Appointment ID: " + appointment.getId() + ", status: " + status + ", isActiveStatus: " + isActiveStatus + ", isNotExcluded: " + isNotExcluded + ", excludeId: " + excludeAppointmentId);
                    return isActiveStatus && isNotExcluded;
                })
                .collect(Collectors.toList());

        System.out.println("[DEBUG] Active conflicting appointments: " + activeAppointments.size());

        // Nếu không có lịch hẹn đang hoạt động nào, bác sĩ khả dụng
        boolean result = activeAppointments.isEmpty();
        System.out.println("[DEBUG] Final availability result: " + result);
        return result;
    }
    public Integer getDoctorIdByUserId(Integer userId) {
        Optional<Doctor> doctorOpt = doctorRepository.findByUserId(userId);
        return doctorOpt.map(Doctor::getId).orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy bác sĩ liên kết với userId: " + userId));
    }


}