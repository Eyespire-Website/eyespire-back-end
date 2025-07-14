package org.eyespire.eyespireapi.dto;

import java.util.List;
import java.util.Map;

public class MedicalRecordDTO {
    private Integer patientId;
    private Integer doctorId;
    private String diagnosis;
    private String notes;
    private Integer appointmentId;
    private List<Integer> serviceIds;
    private List<Map<String, Integer>> productQuantities;

    // Constructors
    public MedicalRecordDTO() {}

    public MedicalRecordDTO(Integer patientId, Integer doctorId, String diagnosis, String notes, 
                           Integer appointmentId, List<Integer> serviceIds, List<Map<String, Integer>> productQuantities) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.diagnosis = diagnosis;
        this.notes = notes;
        this.appointmentId = appointmentId;
        this.serviceIds = serviceIds;
        this.productQuantities = productQuantities;
    }

    // Getters and Setters
    public Integer getPatientId() {
        return patientId;
    }

    public void setPatientId(Integer patientId) {
        this.patientId = patientId;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }

    public List<Integer> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(List<Integer> serviceIds) {
        this.serviceIds = serviceIds;
    }

    public List<Map<String, Integer>> getProductQuantities() {
        return productQuantities;
    }

    public void setProductQuantities(List<Map<String, Integer>> productQuantities) {
        this.productQuantities = productQuantities;
    }

    @Override
    public String toString() {
        return "MedicalRecordDTO{" +
                "patientId=" + patientId +
                ", doctorId=" + doctorId +
                ", diagnosis='" + diagnosis + '\'' +
                ", notes='" + notes + '\'' +
                ", appointmentId=" + appointmentId +
                ", serviceIds=" + serviceIds +
                ", productQuantities=" + productQuantities +
                '}';
    }
}
