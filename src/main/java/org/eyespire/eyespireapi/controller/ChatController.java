package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.service.ChatService;
import org.eyespire.eyespireapi.service.ChatboxSessionService;
import org.eyespire.eyespireapi.model.ChatboxSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final ChatboxSessionService sessionService;

    @Autowired
    public ChatController(ChatService chatService, ChatboxSessionService sessionService) {
        this.chatService = chatService;
        this.sessionService = sessionService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            String userMessage = (String) request.get("message");
            Integer userId = (Integer) request.get("userId");
            
            if (userMessage == null || userMessage.trim().isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Tin nhắn không được để trống");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Generate AI response
            String response = chatService.generateResponse(userMessage);
            
            // Prepare chat history entry
            List<Map<String, Object>> chatHistory = new ArrayList<>();
            
            // Add user message
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            userMsg.put("timestamp", LocalDateTime.now().toString());
            chatHistory.add(userMsg);
            
            // Add bot response
            Map<String, Object> botMsg = new HashMap<>();
            botMsg.put("role", "bot");
            botMsg.put("content", response);
            botMsg.put("timestamp", LocalDateTime.now().toString());
            chatHistory.add(botMsg);
            
            // Save session if userId provided
            if (userId != null) {
                try {
                    Map<String, Object> metadata = sessionService.createSessionMetadata(
                        "CHAT", 
                        Map.of("messageLength", userMessage.length()),
                        httpRequest.getHeader("User-Agent")
                    );
                    
                    sessionService.saveSession(userId, chatHistory, metadata);
                } catch (Exception e) {
                    logger.warn("Failed to save chat session for user: {}", userId, e);
                    // Continue without failing the request
                }
            }
            
            // Prepare response
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("response", response);
            responseMap.put("timestamp", LocalDateTime.now().toString());
            responseMap.put("success", true);
            
            return ResponseEntity.ok(responseMap);
            
        } catch (Exception e) {
            logger.error("Error processing chat message", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Đã xảy ra lỗi khi xử lý tin nhắn");
            errorResponse.put("success", false);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Lấy lịch sử chat của user
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getChatHistory(@PathVariable Integer userId) {
        try {
            List<ChatboxSession> sessions = sessionService.getUserSessions(userId);
            
            List<Map<String, Object>> history = new ArrayList<>();
            
            for (ChatboxSession session : sessions) {
                Map<String, Object> sessionInfo = new HashMap<>();
                sessionInfo.put("id", session.getId());
                sessionInfo.put("createdAt", session.getCreatedAt());
                sessionInfo.put("updatedAt", session.getUpdatedAt());
                
                List<Map<String, Object>> chatHistory = sessionService.getChatHistoryFromSession(session);
                sessionInfo.put("chatHistory", chatHistory);
                
                history.add(sessionInfo);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("history", history);
            response.put("totalSessions", sessions.size());
            response.put("success", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting chat history for user: {}", userId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể lấy lịch sử chat");
            errorResponse.put("success", false);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Xóa lịch sử chat của user
     */
    @DeleteMapping("/history/{userId}")
    public ResponseEntity<?> deleteChatHistory(@PathVariable Integer userId) {
        try {
            sessionService.deleteUserSessions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đã xóa lịch sử chat thành công");
            response.put("success", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting chat history for user: {}", userId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể xóa lịch sử chat");
            errorResponse.put("success", false);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Lấy thống kê chat của user
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<?> getChatStats(@PathVariable Integer userId) {
        try {
            Map<String, Object> stats = sessionService.getSessionStats(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("stats", stats);
            response.put("success", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting chat stats for user: {}", userId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể lấy thống kê chat");
            errorResponse.put("success", false);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
