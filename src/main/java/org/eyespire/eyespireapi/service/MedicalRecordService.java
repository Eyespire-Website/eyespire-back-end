package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.model.*;
import org.eyespire.eyespireapi.model.enums.ProductType;
import org.eyespire.eyespireapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;
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

    @Autowired
    private AppointmentInvoiceService appointmentInvoiceService;

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
            List<Integer> productIds = productQuantities.stream()
                    .map(pq -> pq.get("productId"))
                    .filter(id -> id != null && id > 0)
                    .distinct()
                    .collect(Collectors.toList());
            List<Product> products = productRepository.findAllById(productIds);
            Map<Integer, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            Set<MedicalRecordProduct> recommendedProducts = new HashSet<>();
            for (Map<String, Integer> pq : productQuantities) {
                Integer productId = pq.get("productId");
                Integer quantity = pq.get("quantity");
                if (productId != null && quantity != null && productId > 0 && quantity > 0 && productMap.containsKey(productId)) {
                    Product product = productMap.get(productId);
                    if (product.getType() != ProductType.MEDICINE) {
                        throw new IllegalArgumentException("Chỉ có thể chọn thuốc: " + productId);
                    }
                    MedicalRecordProduct mrp = new MedicalRecordProduct();
                    mrp.setMedicalRecord(record);
                    mrp.setProduct(product);
                    mrp.setQuantity(quantity);
                    recommendedProducts.add(mrp);
                }
            }
            record.setRecommendedProducts(recommendedProducts);
        } else {
            record.setRecommendedProducts(new HashSet<>());
        }

        // Set file URL
        if (files != null && files.length > 0) {
            String fileUrl = fileStorageService.storeFiles(files);
            record.setRecordFileUrl(fileUrl);
        }

        MedicalRecord savedRecord = medicalRecordRepository.save(record);

        // Update invoice if appointment exists
        if (appointmentId != null) {
            BigDecimal totalAmount = BigDecimal.ZERO;
            if (productQuantities != null && !productQuantities.isEmpty()) {
                totalAmount = productQuantities.stream()
                        .map(pq -> {
                            Integer productId = pq.get("productId");
                            Integer quantity = pq.get("quantity");
                            Product product = productRepository.findById(productId).orElse(null);
                            if (product != null) {
                                return product.getPrice().multiply(new BigDecimal(quantity));
                            }
                            return BigDecimal.ZERO;
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            appointmentInvoiceService.updateInvoiceAfterMedicalRecord(appointmentId, totalAmount);
        }

        return savedRecord;
    }

    @Transactional
    public MedicalRecord updateMedicalRecord(Integer recordId, String diagnosis, String notes,
                                             List<Map<String, Integer>> productQuantities, MultipartFile[] files,
                                             List<String> filesToDelete) {
        try {
            System.out.println("Updating medical record ID: " + recordId + ", diagnosis: " + diagnosis +
                    ", notes: " + notes + ", productQuantities: " + (productQuantities != null ? productQuantities : "none") +
                    ", files: " + (files != null ? Arrays.stream(files).map(MultipartFile::getOriginalFilename).collect(Collectors.toList()) : "none") +
                    ", filesToDelete: " + (filesToDelete != null ? filesToDelete : "none"));

            MedicalRecord record = medicalRecordRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ điều trị với ID: " + recordId));

            // Update diagnosis: Allow empty strings and set to "Chưa cập nhật" if empty
            if (diagnosis != null) {
                String trimmedDiagnosis = diagnosis.trim();
                String newDiagnosis = trimmedDiagnosis.isEmpty() ? "Chưa cập nhật" : trimmedDiagnosis;
                // Vietnamese text processing - no conversion needed
                record.setDiagnosis(newDiagnosis);
            }

            // Update notes: Allow empty strings
            if (notes != null) {
                String trimmedNotes = notes.trim();
                // Vietnamese text processing - no conversion needed
                record.setNotes(trimmedNotes);
            }

            // Update recommended products
            if (productQuantities != null) {
                // Load existing MedicalRecordProducts to ensure they are managed
                Set<MedicalRecordProduct> existingProducts = record.getRecommendedProducts();
                if (existingProducts == null) {
                    existingProducts = new HashSet<>();
                    record.setRecommendedProducts(existingProducts);
                }

                // Create a map of existing products by productId for easy lookup
                Map<Integer, MedicalRecordProduct> existingProductMap = existingProducts.stream()
                        .filter(mrp -> mrp.getProduct() != null && mrp.getProduct().getId() != null)
                        .collect(Collectors.toMap(
                                mrp -> mrp.getProduct().getId(),
                                mrp -> mrp,
                                (mrp1, mrp2) -> mrp1 // In case of duplicates, keep the first
                        ));

                // Fetch all required products in one query
                List<Integer> productIds = productQuantities.stream()
                        .map(pq -> pq.get("productId"))
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .collect(Collectors.toList());
                List<Product> products = productRepository.findAllById(productIds);
                Map<Integer, Product> productMap = products.stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));

                // Create a set of new product IDs from the input
                Set<Integer> newProductIds = productQuantities.stream()
                        .map(pq -> pq.get("productId"))
                        .filter(id -> id != null && id > 0)
                        .collect(Collectors.toSet());

                // Remove products that are no longer in the input (orphan removal will handle deletion)
                existingProducts.removeIf(mrp -> {
                    Integer productId = mrp.getProduct() != null ? mrp.getProduct().getId() : null;
                    return productId != null && !newProductIds.contains(productId);
                });

                // Update or add products
                for (Map<String, Integer> pq : productQuantities) {
                    Integer productId = pq.get("productId");
                    Integer quantity = pq.get("quantity");
                    if (productId == null || quantity == null || productId <= 0 || quantity <= 0) {
                        System.err.println("Invalid product quantity: productId=" + productId + ", quantity=" + quantity);
                        continue;
                    }
                    Product product = productMap.get(productId);
                    if (product == null) {
                        System.err.println("Product not found: productId=" + productId);
                        continue;
                    }
                    if (product.getType() != ProductType.MEDICINE) {
                        throw new IllegalArgumentException("Chỉ có thể chọn thuốc: " + productId);
                    }

                    // Check if the product already exists in the collection
                    MedicalRecordProduct mrp = existingProductMap.get(productId);
                    if (mrp != null) {
                        // Update existing product quantity
                        mrp.setQuantity(quantity);
                    } else {
                        // Add new product
                        MedicalRecordProduct newMrp = new MedicalRecordProduct();
                        newMrp.setMedicalRecord(record);
                        newMrp.setProduct(product);
                        newMrp.setQuantity(quantity);
                        existingProducts.add(newMrp);
                    }
                }
            } else {
                // If productQuantities is null or empty, clear the collection
                if (record.getRecommendedProducts() != null) {
                    record.getRecommendedProducts().clear();
                }
            }

            // Update files
            if (files != null && files.length > 0) {
                try {
                    String newFileUrl = fileStorageService.storeFiles(files);
                    String existingFileUrl = record.getRecordFileUrl();
                    if (existingFileUrl != null && !existingFileUrl.isEmpty()) {
                        record.setRecordFileUrl(existingFileUrl + ";" + newFileUrl);
                    } else {
                        record.setRecordFileUrl(newFileUrl);
                    }
                } catch (Exception e) {
                    System.err.println("Error storing files: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("Failed to store files: " + e.getMessage());
                }
            }

            // Delete files
            if (filesToDelete != null && !filesToDelete.isEmpty()) {
                String currentFileUrl = record.getRecordFileUrl();
                if (currentFileUrl != null && !currentFileUrl.isEmpty()) {
                    List<String> currentFiles = Arrays.asList(currentFileUrl.split(";"));
                    List<String> updatedFiles = currentFiles.stream()
                            .filter(url -> {
                                String normalizedUrl = url.startsWith("/uploads/") ? url : "/uploads/" + url;
                                return !filesToDelete.contains(normalizedUrl) && !filesToDelete.contains(url);
                            })
                            .collect(Collectors.toList());
                    String updatedFileUrl = String.join(";", updatedFiles);
                    record.setRecordFileUrl(updatedFileUrl.isEmpty() ? null : updatedFileUrl);
                    for (String fileUrl : filesToDelete) {
                        try {
                            String normalizedFileUrl = fileUrl.startsWith("/uploads/") ? fileUrl : "/uploads/" + fileUrl;
                            fileStorageService.deleteFile(normalizedFileUrl);
                        } catch (Exception e) {
                            System.err.println("Error deleting file: " + fileUrl + ", error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }

            // Save the record before updating the invoice to avoid premature flush
            // Debug UTF-8 before database save
            try {
                System.out.println("[PRE-SAVE DEBUG] Diagnosis: " + record.getDiagnosis());
                System.out.println("[PRE-SAVE DEBUG] Diagnosis bytes: " + java.util.Arrays.toString(record.getDiagnosis().getBytes("UTF-8")));
                System.out.println("[PRE-SAVE DEBUG] Notes: " + record.getNotes());
                System.out.println("[PRE-SAVE DEBUG] Notes bytes: " + java.util.Arrays.toString(record.getNotes().getBytes("UTF-8")));
            } catch (Exception e) {
                System.err.println("Error in UTF-8 debug: " + e.getMessage());
            }
            MedicalRecord savedRecord = medicalRecordRepository.save(record);

            // Update invoice if appointment exists
            if (record.getAppointment() != null) {
                BigDecimal totalAmount = BigDecimal.ZERO;
                if (productQuantities != null && !productQuantities.isEmpty()) {
                    totalAmount = productQuantities.stream()
                            .map(pq -> {
                                Integer productId = pq.get("productId");
                                Integer quantity = pq.get("quantity");
                                Product product = productRepository.findById(productId).orElse(null);
                                if (product != null) {
                                    return product.getPrice().multiply(new BigDecimal(quantity));
                                }
                                return BigDecimal.ZERO;
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                appointmentInvoiceService.updateInvoiceAfterMedicalRecord(record.getAppointment().getId(), totalAmount);
            }

            System.out.println("Saved medical record: " + savedRecord);
            // Debug UTF-8 after database save
            try {
                System.out.println("[POST-SAVE DEBUG] Diagnosis from DB: " + savedRecord.getDiagnosis());
                System.out.println("[POST-SAVE DEBUG] Diagnosis bytes from DB: " + java.util.Arrays.toString(savedRecord.getDiagnosis().getBytes("UTF-8")));
                System.out.println("[POST-SAVE DEBUG] Notes from DB: " + savedRecord.getNotes());
                System.out.println("[POST-SAVE DEBUG] Notes bytes from DB: " + java.util.Arrays.toString(savedRecord.getNotes().getBytes("UTF-8")));
            } catch (Exception e) {
                System.err.println("Error in post-save UTF-8 debug: " + e.getMessage());
            }
            return savedRecord;
        } catch (Exception e) {
            System.err.println("Error updating medical record ID: " + recordId + ", message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update medical record: " + e.getMessage());
        }
    }

    public List<Product> getProductsMedicine() {
        return productRepository.findAllByType(ProductType.MEDICINE);
    }

    public List<MedicalRecord> getDoctorMedicalRecordsByUserId(Integer userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bác sĩ với userId: " + userId));
        return medicalRecordRepository.findByDoctorId(doctor.getId());
    }

    public List<MedicalRecord> getPatientMedicalRecordsByUserId(Integer userId) {
        User patient = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân với userId: " + userId));
        
        System.out.println("[PATIENT RECORDS DEBUG] Getting medical records for patient ID: " + userId);
        
        // Get ALL medical records for this patient first (for debugging)
        List<MedicalRecord> allRecords = medicalRecordRepository.findByPatientId(userId);
        System.out.println("[PATIENT RECORDS DEBUG] Total medical records for patient: " + allRecords.size());
        
        for (MedicalRecord record : allRecords) {
            String appointmentStatus = record.getAppointment() != null ? record.getAppointment().getStatus().toString() : "NO_APPOINTMENT";
            System.out.println("[PATIENT RECORDS DEBUG] Record ID: " + record.getId() + 
                             ", Appointment ID: " + (record.getAppointment() != null ? record.getAppointment().getId() : "null") +
                             ", Status: " + appointmentStatus +
                             ", Diagnosis: " + record.getDiagnosis());
        }
        
        // Chỉ trả về hồ sơ bệnh án của các cuộc hẹn đã hoàn tất (COMPLETED)
        List<MedicalRecord> completedRecords = medicalRecordRepository.findByPatientIdAndAppointmentStatus(userId, org.eyespire.eyespireapi.model.enums.AppointmentStatus.COMPLETED);
        System.out.println("[PATIENT RECORDS DEBUG] COMPLETED records returned: " + completedRecords.size());
        
        return completedRecords;
    }

    public Optional<MedicalRecord> getMedicalRecordById(Integer recordId) {
        return medicalRecordRepository.findById(recordId);
    }

    public boolean existsByAppointmentId(Integer appointmentId) {
        return medicalRecordRepository.existsByAppointmentId(appointmentId);
    }

    public Optional<MedicalRecord> getMedicalRecordByAppointmentId(Integer appointmentId) {
        return medicalRecordRepository.findByAppointmentId(appointmentId);
    }
}