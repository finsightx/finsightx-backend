package com.finsightx.finsightx_backend.controller;

import com.finsightx.finsightx_backend.dto.request.ChatbotRequest;
import com.finsightx.finsightx_backend.dto.response.ChatbotResponse;
import com.finsightx.finsightx_backend.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/send")
    public ResponseEntity<ChatbotResponse> sendMessage(@RequestBody ChatbotRequest request) {
        ChatbotResponse response = chatbotService.sendMessage(request.getMessage());
        return ResponseEntity.ok(response);
    }

}
