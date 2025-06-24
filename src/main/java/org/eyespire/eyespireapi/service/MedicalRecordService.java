package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.model.MedicalRecord;
import org.eyespire.eyespireapi.model.MedicalRecordProduct;
import org.eyespire.eyespireapi.model.Product;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.ProductType;
import org.eyespire.eyespireapi.repository.AppointmentRepository;
import org.eyespire.eyespireapi.repository.DoctorRepository;
import org.eyespire.eyespireapi.repository.MedicalRecordRepository;
import org.eyespire.eyespireapi.repository.ProductRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MedicalRecordService {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Transactional
    public MedicalRecord createMedicalRecord(Integer patientId, Integer doctorId, String diagnosis,
                                             String notes, Integer appointmentId, List<Map<String, Integer>> productQuantities,
                                             MultipartFile[] files) {
        MedicalRecord record = new MedicalRecord();

        // Set patient
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Bệnh nhân không tồn tại với ID: " + patientId));
        record.setPatient(patient);

        // Set doctor
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Bác sĩ không tồn tại với ID: " + doctorId));
        record.setDoctor(doctor);

        // Set appointment
        if (appointmentId != null) {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuộc hẹn không tồn tại với ID: " + appointmentId));
            record.setAppointment(appointment);
        }

        record.setDiagnosis(diagnosis);
        record.setNotes(notes);

        // Set recommended products
        if (productQuantities != null && !productQuantities.isEmpty()) {
            Set<MedicalRecordProduct> recommendedProducts = productQuantities.stream()
                    .map(pq -> {
                        Integer productId = pq.get("productId");
                        Integer quantity = pq.get("quantity");
                        Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new IllegalArgumentException("Thuốc không tồn tại với ID: " + productId));
                        if (product.getType() != ProductType.MEDICINE) {
                            throw new IllegalArgumentException("Chỉ có thể chọn thuốc: " + productId);
                        }
                        MedicalRecordProduct mrp = new MedicalRecordProduct();
                        mrp.setMedicalRecord(record);
                        mrp.setProduct(product);
                        mrp.setQuantity(quantity);
                        return mrp;
                    })
                    .collect(Collectors.toSet());
            record.setRecommendedProducts(recommendedProducts);
        }

        // Set file URL
        if (files != null && files.length > 0) {
            String fileUrl = fileStorageService.storeFiles(files);
            record.setRecordFileUrl(fileUrl);
        }

        return medicalRecordRepository.save(record);
    }

    public List<Product> getProductsMedicine() {
        return productRepository.findAllByType(ProductType.MEDICINE);
    }

    public List<MedicalRecord> getDoctorMedicalRecordsByUserId(Integer userId) {
        // Map userId to doctorId
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bác sĩ với userId: " + userId));
        System.out.println("Mapped userId " + userId + " to doctorId: " + doctor.getId());
        List<MedicalRecord> records = medicalRecordRepository.findByDoctorId(doctor.getId());
        System.out.println("Found " + records.size() + " records for doctorId: " + doctor.getId());
        return records;
    }
    public boolean existsByAppointmentId(Integer appointmentId) {
        return medicalRecordRepository.existsByAppointmentId(appointmentId);
    }

}