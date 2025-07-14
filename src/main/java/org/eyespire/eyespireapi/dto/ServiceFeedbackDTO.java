package org.eyespire.eyespireapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class ServiceFeedbackDTO {

    private Integer id;

    @JsonProperty("appointmentId")
    private Integer appointmentId;

    @JsonProperty("patientId")
    private Integer patientId;

    @JsonProperty("rating")
    private Integer rating;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    // Constructors
    public ServiceFeedbackDTO() {}

    public ServiceFeedbackDTO(Integer appointmentId, Integer patientId, Integer rating, String comment) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.rating = rating;
        this.comment = comment;
    }

    public ServiceFeedbackDTO(Integer id, Integer appointmentId, Integer patientId, 
                            Integer rating, String comment, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public void setPatientId(Integer patientId) {
        this.patientId = patientId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // toString method
    @Override
    public String toString() {
        return "ServiceFeedbackDTO{" +
                "id=" + id +
                ", appointmentId=" + appointmentId +
                ", patientId=" + patientId +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // Validation helper methods
    public boolean isValidRating() {
        return rating != null && rating >= 1 && rating <= 5;
    }

    public boolean hasComment() {
        return comment != null && !comment.trim().isEmpty();
    }

    public boolean isValidComment() {
        return comment == null || comment.length() <= 1000;
    }
}