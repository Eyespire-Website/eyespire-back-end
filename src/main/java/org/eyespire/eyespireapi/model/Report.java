package org.eyespire.eyespireapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eyespire.eyespireapi.model.enums.ReportType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reports")
public class Report {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 20)
    private ReportType reportType;
    
    @ManyToOne
    @JoinColumn(name = "generated_by")
    private User generatedBy;
    
    @Column(name = "report_data")
    private String reportData;
    
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
    
    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}
