package org.eyespire.eyespireapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eyespire.eyespireapi.model.Message;
import org.eyespire.eyespireapi.repository.MessageRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.eyespire.eyespireapi.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<Message>> getMessages(@PathVariable("userId") Integer userId) {
        try {
            List<Message> messages = messageRepository.findBySender_IdOrReceiver_Id(userId, userId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Message> sendMessage(
            @RequestPart(value = "message") String messageJson,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        try {
            Message message = objectMapper.readValue(messageJson, Message.class);
            if (message.getSender() == null || message.getReceiver() == null) {
                return ResponseEntity.badRequest().build();
            }
            if (images != null && images.length > 0) {
                message.setImageUrls(fileStorageService.storeFiles(images));
            }
            if (message.getContent() == null || message.getContent().isEmpty()) {
                message.setContent(""); // Ensure content is not null
            }
            message.setSentAt(LocalDateTime.now());
            message.setIsRead(false);
            Message savedMessage = messageRepository.save(message);
            messagingTemplate.convertAndSendToUser(
                    savedMessage.getReceiverId().toString(),
                    "/queue/messages",
                    savedMessage
            );
            return ResponseEntity.ok(savedMessage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Message> markAsRead(@PathVariable("id") Integer messageId) {
        try {
            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("Message not found"));
            message.setIsRead(true);
            Message updatedMessage = messageRepository.save(message);
            messagingTemplate.convertAndSendToUser(
                    updatedMessage.getSenderId().toString(),
                    "/queue/messages/read",
                    updatedMessage
            );
            return ResponseEntity.ok(updatedMessage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @MessageMapping("/chat")
    public void handleChatMessage(@Payload Object payload) {
        try {
            Message message = objectMapper.convertValue(payload, Message.class);
            if (message == null || message.getSender() == null || message.getReceiver() == null) {
                throw new IllegalArgumentException("Invalid message payload");
            }
            if (message.getContent() == null) {
                message.setContent("");
            }
            message.setSentAt(LocalDateTime.now());
            message.setIsRead(false);
            Message savedMessage = messageRepository.save(message);
            messagingTemplate.convertAndSendToUser(
                    savedMessage.getReceiverId().toString(),
                    "/queue/messages",
                    savedMessage
            );
        } catch (Exception e) {
            throw new RuntimeException("Error handling WebSocket message: " + e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.read")
    public void handleReadStatus(@Payload Object payload) {
        try {
            Message message = objectMapper.convertValue(payload, Message.class);
            if (message == null || message.getId() == null) {
                throw new IllegalArgumentException("Invalid read status payload");
            }
            Message existingMessage = messageRepository.findById(message.getId())
                    .orElseThrow(() -> new RuntimeException("Message not found"));
            existingMessage.setIsRead(true);
            Message updatedMessage = messageRepository.save(existingMessage);
            messagingTemplate.convertAndSendToUser(
                    updatedMessage.getSenderId().toString(),
                    "/queue/messages/read",
                    updatedMessage
            );
        } catch (Exception e) {
            throw new RuntimeException("Error handling read status: " + e.getMessage(), e);
        }
    }
}