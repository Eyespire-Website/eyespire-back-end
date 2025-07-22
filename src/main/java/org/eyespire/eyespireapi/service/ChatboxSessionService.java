package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.model.ChatboxSession;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.repository.ChatboxSessionRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ChatboxSessionService {

    private static final Logger logger = LoggerFactory.getLogger(ChatboxSessionService.class);
    
    @Autowired
    private ChatboxSessionRepository chatboxSessionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Giới hạn số session lưu trữ cho mỗi user
    private static final int MAX_SESSIONS_PER_USER = 10;

    /**
     * Lưu session chat mới
     */
    public ChatboxSession saveSession(Integer userId, List<Map<String, Object>> chatHistory, Map<String, Object> metadata) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                throw new IllegalArgumentException("User not found: " + userId);
            }
            
            User user = userOpt.get();
            
            // Tạo session data JSON
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("chatHistory", chatHistory);
            sessionData.put("metadata", metadata);
            sessionData.put("timestamp", LocalDateTime.now().toString());
            
            String sessionDataJson = objectMapper.writeValueAsString(sessionData);
            
            ChatboxSession session = new ChatboxSession();
            session.setUser(user);
            session.setSessionData(sessionDataJson);
            
            ChatboxSession savedSession = chatboxSessionRepository.save(session);
            
            // Cleanup old sessions
            cleanupOldSessions(userId);
            
            logger.info("Saved chat session for user: {}", userId);
            return savedSession;
            
        } catch (Exception e) {
            logger.error("Error saving chat session for user: {}", userId, e);
            throw new RuntimeException("Failed to save chat session", e);
        }
    }

    /**
     * Lấy session gần nhất của user
     */
    public Optional<ChatboxSession> getLatestSession(Integer userId) {
        try {
            return chatboxSessionRepository.findFirstByUserIdOrderByUpdatedAtDesc(userId);
        } catch (Exception e) {
            logger.error("Error getting latest session for user: {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Lấy tất cả session của user
     */
    public List<ChatboxSession> getUserSessions(Integer userId) {
        try {
            return chatboxSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        } catch (Exception e) {
            logger.error("Error getting sessions for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Parse session data từ JSON
     */
    public Map<String, Object> parseSessionData(String sessionDataJson) {
        try {
            if (sessionDataJson == null || sessionDataJson.trim().isEmpty()) {
                return Collections.emptyMap();
            }
            
            return objectMapper.readValue(sessionDataJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.error("Error parsing session data: {}", sessionDataJson, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Lấy chat history từ session
     */
    public List<Map<String, Object>> getChatHistoryFromSession(ChatboxSession session) {
        try {
            Map<String, Object> sessionData = parseSessionData(session.getSessionData());
            Object chatHistory = sessionData.get("chatHistory");
            
            if (chatHistory instanceof List) {
                return (List<Map<String, Object>>) chatHistory;
            }
            
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error extracting chat history from session: {}", session.getId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Cập nhật session với chat history mới
     */
    public ChatboxSession updateSession(Integer sessionId, List<Map<String, Object>> newChatHistory) {
        try {
            Optional<ChatboxSession> sessionOpt = chatboxSessionRepository.findById(sessionId);
            if (!sessionOpt.isPresent()) {
                throw new IllegalArgumentException("Session not found: " + sessionId);
            }
            
            ChatboxSession session = sessionOpt.get();
            Map<String, Object> sessionData = parseSessionData(session.getSessionData());
            
            // Update chat history
            sessionData.put("chatHistory", newChatHistory);
            sessionData.put("lastUpdated", LocalDateTime.now().toString());
            
            String updatedSessionDataJson = objectMapper.writeValueAsString(sessionData);
            session.setSessionData(updatedSessionDataJson);
            
            return chatboxSessionRepository.save(session);
            
        } catch (Exception e) {
            logger.error("Error updating session: {}", sessionId, e);
            throw new RuntimeException("Failed to update session", e);
        }
    }

    /**
     * Xóa session cũ để giữ số lượng session trong giới hạn
     */
    private void cleanupOldSessions(Integer userId) {
        try {
            long sessionCount = chatboxSessionRepository.countByUserId(userId);
            
            if (sessionCount > MAX_SESSIONS_PER_USER) {
                chatboxSessionRepository.deleteOldSessions(userId, MAX_SESSIONS_PER_USER);
                logger.info("Cleaned up old sessions for user: {}", userId);
            }
        } catch (Exception e) {
            logger.warn("Error cleaning up old sessions for user: {}", userId, e);
            // Don't throw exception, just log warning
        }
    }

    /**
     * Xóa tất cả session của user
     */
    public void deleteUserSessions(Integer userId) {
        try {
            List<ChatboxSession> sessions = chatboxSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
            chatboxSessionRepository.deleteAll(sessions);
            logger.info("Deleted all sessions for user: {}", userId);
        } catch (Exception e) {
            logger.error("Error deleting sessions for user: {}", userId, e);
            throw new RuntimeException("Failed to delete user sessions", e);
        }
    }

    /**
     * Tạo session metadata
     */
    public Map<String, Object> createSessionMetadata(String queryType, Map<String, Object> queryParameters, String userAgent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("queryType", queryType);
        metadata.put("queryParameters", queryParameters);
        metadata.put("userAgent", userAgent);
        metadata.put("sessionStart", LocalDateTime.now().toString());
        return metadata;
    }

    /**
     * Thống kê session theo user
     */
    public Map<String, Object> getSessionStats(Integer userId) {
        try {
            List<ChatboxSession> sessions = getUserSessions(userId);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSessions", sessions.size());
            stats.put("lastSessionDate", sessions.isEmpty() ? null : sessions.get(0).getUpdatedAt());
            
            // Analyze query types from sessions
            Map<String, Integer> queryTypeStats = new HashMap<>();
            for (ChatboxSession session : sessions) {
                Map<String, Object> sessionData = parseSessionData(session.getSessionData());
                Map<String, Object> metadata = (Map<String, Object>) sessionData.get("metadata");
                
                if (metadata != null && metadata.containsKey("queryType")) {
                    String queryType = (String) metadata.get("queryType");
                    queryTypeStats.put(queryType, queryTypeStats.getOrDefault(queryType, 0) + 1);
                }
            }
            
            stats.put("queryTypeStats", queryTypeStats);
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting session stats for user: {}", userId, e);
            return Collections.emptyMap();
        }
    }
}
