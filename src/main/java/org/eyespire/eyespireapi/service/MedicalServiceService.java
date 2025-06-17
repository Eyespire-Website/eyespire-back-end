package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.model.MedicalService;
import org.eyespire.eyespireapi.repository.MedicalServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicalServiceService {

    @Autowired
    private MedicalServiceRepository medicalServiceRepository;

    /**
     * Lấy danh sách tất cả dịch vụ y tế
     */
    public List<MedicalService> getAllMedicalServices() {
        return medicalServiceRepository.findAll();
    }

    /**
     * Lấy thông tin chi tiết dịch vụ y tế theo ID
     */
    public MedicalService getMedicalServiceById(Integer id) {
        return medicalServiceRepository.findById(id).orElse(null);
    }

    /**
     * Lấy danh sách dịch vụ y tế theo loại
     */
    public List<MedicalService> getMedicalServicesByCategory(Integer categoryId) {
        return medicalServiceRepository.findByCategoryId(categoryId);
    }

    /**
     * Lấy danh sách dịch vụ y tế của bác sĩ
     */
    public List<MedicalService> getMedicalServicesByDoctor(Integer doctorId) {
        return medicalServiceRepository.findByDoctorId(doctorId);
    }
}
