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
     * Lấy danh sách dịch vụ y tế của bác sĩ
     */
    public List<MedicalService> getMedicalServicesByDoctor(Integer doctorId) {
        return medicalServiceRepository.findByDoctorId(doctorId);
    }

    /**
     * Tạo mới dịch vụ y tế
     */
    public MedicalService createMedicalService(MedicalService medicalService) {
        return medicalServiceRepository.save(medicalService);
    }

    /**
     * Cập nhật thông tin dịch vụ y tế
     */
    public MedicalService updateMedicalService(Integer id, MedicalService medicalServiceDetails) {
        MedicalService existingService = getMedicalServiceById(id);
        if (existingService == null) {
            return null;
        }
        
        existingService.setName(medicalServiceDetails.getName());
        existingService.setDescription(medicalServiceDetails.getDescription());
        existingService.setPrice(medicalServiceDetails.getPrice());
        existingService.setImageUrl(medicalServiceDetails.getImageUrl());
        existingService.setDuration(medicalServiceDetails.getDuration());
        
        return medicalServiceRepository.save(existingService);
    }

    /**
     * Xóa dịch vụ y tế
     */
    public boolean deleteMedicalService(Integer id) {
        MedicalService existingService = getMedicalServiceById(id);
        if (existingService == null) {
            return false;
        }
        
        medicalServiceRepository.delete(existingService);
        return true;
    }

    /**
     * Tìm kiếm dịch vụ y tế theo tên
     */
    public List<MedicalService> searchMedicalServices(String keyword) {
        return medicalServiceRepository.findByNameContainingIgnoreCase(keyword);
    }
}
