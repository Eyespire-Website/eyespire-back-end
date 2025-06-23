package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        
        if (userMessage == null || userMessage.trim().isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Tin nhắn không được để trống");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        String response = chatService.generateResponse(userMessage);
        
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("response", response);
        
        return ResponseEntity.ok(responseMap);
    }
}
