package com.ragapp.controller;

import com.ragapp.model.ChatRequest;
import com.ragapp.model.ChatResponse;
import com.ragapp.model.IngestResponse;
import com.ragapp.service.DocumentIngestionService;
import com.ragapp.service.RagChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * RagController — exposes REST endpoints + serves the Thymeleaf chat UI.
 *
 * Endpoints:
 *   GET  /                    → chat UI (Thymeleaf)
 *   POST /api/ingest          → upload & ingest document
 *   POST /api/chat            → ask a question (RAG)
 *
 * ─── Future Agent endpoint (Phase 2) ─────────────────────────────────────
 *   POST /api/agent/chat      → agent-enhanced chat with tools
 * ──────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RagController {

    private final DocumentIngestionService ingestionService;
    private final RagChatService chatService;

    // ─── UI ──────────────────────────────────────────────────────────────────

    @GetMapping("/")
    public String chatUI(Model model) {
        model.addAttribute("sessionId", UUID.randomUUID().toString());
        return "chat"; // → templates/chat.html
    }

    // ─── Ingest ──────────────────────────────────────────────────────────────

    /**
     * POST /api/ingest
     * Upload a PDF or text file to be split, embedded, and stored.
     *
     * curl example:
     *   curl -X POST http://localhost:8080/api/ingest \
     *        -F "file=@my-document.pdf"
     */
    @PostMapping("/api/ingest")
    @ResponseBody
    public ResponseEntity<IngestResponse> ingest(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new IngestResponse(false, "No file provided", 0));
        }

        String filename = file.getOriginalFilename();
        String ct = file.getContentType() != null ? file.getContentType() : "";

        // Validate supported types
        if (!ct.contains("pdf") && !ct.contains("text") && !filename.endsWith(".md")) {
            return ResponseEntity.badRequest().body(
                    new IngestResponse(false,
                            "Unsupported file type. Use PDF, .txt, or .md", 0));
        }

        try {
            int chunks = ingestionService.ingest(file);
            return ResponseEntity.ok(
                    new IngestResponse(true,
                            "Ingested '" + filename + "' → " + chunks + " chunks stored", chunks));
        } catch (Exception e) {
            log.error("Ingestion failed for {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new IngestResponse(false, "Ingestion failed: " + e.getMessage(), 0));
        }
    }

    // ─── Chat ─────────────────────────────────────────────────────────────────

    /**
     * POST /api/chat
     * Ask a question — retrieves relevant chunks and returns an LLM answer.
     *
     * curl example:
     *   curl -X POST http://localhost:8080/api/chat \
     *        -H "Content-Type: application/json" \
     *        -d '{"question":"What is the refund policy?","sessionId":"abc123"}'
     */
    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        try {
            String answer = chatService.chat(request.question(), request.sessionId());
            return ResponseEntity.ok(new ChatResponse(true, answer, null));
        } catch (Exception e) {
            log.error("Chat failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse(false, null, "Chat failed: " + e.getMessage()));
        }
    }
}
