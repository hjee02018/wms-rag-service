package com.github.hjyang.rag.wms_rag_service.controller;

import com.github.hjyang.rag.wms_rag_service.service.DocumentVectorService;
import com.github.hjyang.rag.wms_rag_service.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rag")
public class ChatController {

    private final ChatClient chatClient;
    private final RagService ragService;
    private final DocumentVectorService documentVectorService;

    // Ollama 연결 확인
    @GetMapping("/ping")
    public String ping() {
        return chatClient.prompt()
                .user("한 문장으로 자기소개해줘")
                .call()
                .content();
    }

    // RAG 질의응답 (DB + 벡터 통합)
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestBody Map<String, String> req) {
        String question = req.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "question is required"));
        }
        return ResponseEntity.ok(Map.of("answer", ragService.ask(question)));
    }

    // PDF 업로드 → 벡터화
    @PostMapping("/documents/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            documentVectorService.addDocument(file);  // addPdf → addDocument 로 변경
            return ResponseEntity.ok("업로드 완료: " + file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("오류: " + e.getMessage());
        }
    }

    // 헬스체크
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}