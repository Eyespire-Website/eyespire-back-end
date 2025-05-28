package org.eyespire.eyespireapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages")
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;
    
    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;
    
    @Column
    private String content;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}
