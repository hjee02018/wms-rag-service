package com.github.hjyang.rag.wms_rag_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rag")
public class ChatController {

    private final ChatClient chatClient;

    @GetMapping("/ping")
    public String ping() {
        return chatClient.prompt()
                .user("한 문장으로 자기소개해줘")
                .call()
                .content();
    }
}