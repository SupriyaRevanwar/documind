<div align="center">

# 🧠 DocuMind

### Chat with your documents using local AI — no cloud, no API keys, no data leaving your machine.

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M3-brightgreen?style=flat-square&logo=spring)](https://spring.io/projects/spring-ai)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.35.0-blue?style=flat-square)](https://github.com/langchain4j/langchain4j)
[![Ollama](https://img.shields.io/badge/Ollama-llama3-black?style=flat-square)](https://ollama.ai/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

[Features](#-features) • [Architecture](#-architecture) • [Quick Start](#-quick-start) • [API](#-rest-api) • [Roadmap](#-roadmap)

![DocuMind Screenshot](https://placehold.co/900x500/0d0f14/7c6af7?text=DocuMind+Chat+UI&font=mono)

</div>

---

## ✨ Features

- 📄 **Upload PDFs and text files** — ingested, chunked, and embedded locally
- 🔍 **Semantic search** — finds relevant content by meaning, not just keywords
- 🤖 **Local LLM** — powered by Ollama (llama3, phi3, mistral — your choice)
- 🔒 **100% private** — no data sent to OpenAI, Anthropic, or any cloud
- 💾 **Persistent vector store** — documents survive app restarts
- 🎨 **Clean chat UI** — built-in Thymeleaf interface, no frontend setup needed
- 🔌 **REST API** — integrate with any frontend or tool
- 🧩 **Dual framework** — Spring AI + LangChain4j, each used for what it does best

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      INGESTION FLOW                         │
│                                                             │
│  Upload PDF / TXT / MD                                      │
│         │                                                   │
│         ▼                                                   │
│  Spring AI PagePdfDocumentReader  ──or──                    │
│  LangChain4j ApacheTikaDocumentParser                       │
│         │  raw text + metadata                              │
│         ▼                                                   │
│  LangChain4j RecursiveCharacterTextSplitter                 │
│         │  chunks (500 chars, 50 overlap)                   │
│         ▼                                                   │
│  Ollama nomic-embed-text  →  768-dim vectors                │
│         │                                                   │
│         ▼                                                   │
│  Spring AI SimpleVectorStore  →  vectorstore.json           │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                        CHAT FLOW                            │
│                                                             │
│  User Question                                              │
│         │                                                   │
│         ▼                                                   │
│  Embed question  →  similarity search  →  top-5 chunks      │
│         │                                                   │
│         ▼                                                   │
│  SYSTEM prompt + retrieved context + question               │
│         │                                                   │
│         ▼                                                   │
│  Ollama llama3  →  grounded answer                          │
└─────────────────────────────────────────────────────────────┘
```

### Framework Division

| Responsibility | Framework | Why |
|---|---|---|
| PDF loading | Spring AI | Page-by-page with metadata |
| TXT / MD loading | LangChain4j | Apache Tika — 1000+ formats |
| Text splitting | LangChain4j | Recursive splitter with overlap |
| Embeddings | Spring AI | Auto-configured Ollama integration |
| Vector store | Spring AI | `SimpleVectorStore` + `QuestionAnswerAdvisor` |
| Chat / LLM | Spring AI | Fluent `ChatClient` + Advisor pattern |

---

## ⚡ Quick Start

### Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java | 21+ | Required |
| Maven | 3.9+ | Or use included `./mvnw` |
| Docker Desktop | Latest | For Ollama |

### 1. Start Ollama

```bash
docker-compose up -d
```

### 2. Pull models (one-time, ~5GB total)

```bash
docker exec ollama ollama pull llama3          # chat model  ~4.7GB
docker exec ollama ollama pull nomic-embed-text # embeddings  ~274MB
```

> **Low RAM?** Use `phi3` instead of `llama3` — 3x faster, ~2.3GB
> ```bash
> docker exec ollama ollama pull phi3
> # then change model: phi3 in application.yml
> ```

### 3. Run the app

```bash
mvn spring-boot:run
```

### 4. Open the chat UI

```
http://localhost:8080
```

Upload a PDF or text file → click **Ingest Document** → start asking questions.

---

## 📁 Project Structure

```
documind/
├── docker-compose.yml                          # Ollama container
├── pom.xml                                     # Spring AI + LangChain4j deps
└── src/main/
    ├── java/com/ragapp/
    │   ├── RagChatbotApplication.java           # Entry point
    │   ├── config/
    │   │   └── RagConfig.java                  # SimpleVectorStore + splitter beans
    │   ├── controller/
    │   │   └── RagController.java              # REST endpoints + UI route
    │   ├── service/
    │   │   ├── DocumentIngestionService.java   # Load → split → embed → store
    │   │   └── RagChatService.java             # RAG query pipeline
    │   └── model/
    │       ├── ChatRequest.java
    │       ├── ChatResponse.java
    │       └── IngestResponse.java
    └── resources/
        ├── application.yml                     # Ollama + RAG config
        └── templates/
            └── chat.html                       # Thymeleaf chat UI
```

---

## ⚙️ Configuration

All tunable parameters in `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          model: llama3        # swap to phi3, mistral, gemma2, etc.
          temperature: 0.7     # 0.0 = deterministic, 1.0 = creative
          num-ctx: 4096        # context window size

rag:
  top-k: 5          # chunks retrieved per question (higher = more context)
  chunk-size: 500   # characters per chunk
  chunk-overlap: 50 # overlap between chunks (~10% of chunk-size)

vectorstore:
  path: ./vectorstore.json   # where vectors are persisted
```

### Supported Models

| Model | Size | Speed (CPU) | Quality | Command |
|---|---|---|---|---|
| llama3 | 4.7GB | Slow | ⭐⭐⭐⭐⭐ | `ollama pull llama3` |
| phi3 | 2.3GB | Fast | ⭐⭐⭐⭐ | `ollama pull phi3` |
| mistral | 4.1GB | Medium | ⭐⭐⭐⭐ | `ollama pull mistral` |
| gemma2 | 5.4GB | Slow | ⭐⭐⭐⭐⭐ | `ollama pull gemma2` |

---

## 🔌 REST API

### Ingest a document

```bash
POST /api/ingest
Content-Type: multipart/form-data

curl -X POST http://localhost:8080/api/ingest \
     -F "file=@your-document.pdf"
```

**Response:**
```json
{
  "success": true,
  "message": "Ingested 'your-document.pdf' → 42 chunks stored",
  "chunks": 42
}
```

### Chat

```bash
POST /api/chat
Content-Type: application/json

curl -X POST http://localhost:8080/api/chat \
     -H "Content-Type: application/json" \
     -d '{"question": "What is the refund policy?", "sessionId": "abc123"}'
```

**Response:**
```json
{
  "success": true,
  "answer": "According to the document, refunds are processed within 7 working days...",
  "error": null
}
```

### Supported File Types

| Format | Loader |
|---|---|
| `.pdf` | Spring AI `PagePdfDocumentReader` |
| `.txt` | LangChain4j Apache Tika |
| `.md` | LangChain4j Apache Tika |

---

## 🗺 Roadmap

- [x] PDF and text ingestion
- [x] Local LLM via Ollama
- [x] Persistent vector store
- [x] REST API + chat UI
- [ ] **Phase 2 — Agents** — LangChain4j `@Tool` for web search, calculator, DB lookup
- [ ] Multi-document management (list, delete individual docs)
- [ ] Conversation memory (multi-turn chat history)
- [ ] Support for `.docx`, `.html`, web URLs
- [ ] Swap SimpleVectorStore → Chroma / pgvector for production scale
- [ ] Docker image for one-command deployment
- [ ] Streaming responses (token-by-token)

---

## 🧠 How RAG Works

RAG (Retrieval-Augmented Generation) is not fine-tuning or prompt engineering. It's a runtime technique:

1. **Ingest** — your document is split into chunks and each chunk is converted to a vector (list of numbers representing meaning)
2. **Query** — your question is also converted to a vector
3. **Retrieve** — the top-N most similar chunks are found using cosine similarity
4. **Generate** — those chunks are injected into the LLM prompt as context
5. **Answer** — the LLM answers *only from the provided context*

The model never learns from your documents (no training). It just reads the relevant chunks at query time — like giving someone a cheat sheet before an exam.

---

## 🤝 Contributing

Pull requests are welcome. For major changes, open an issue first.

```bash
git clone https://github.com/surevanwar/documind.git
cd documind
mvn spring-boot:run
```

---

## 📄 License

[MIT](LICENSE) © surevanwar

---

<div align="center">
Built with ☕ Java · 🍃 Spring AI · ⛓ LangChain4j · 🦙 Ollama
</div>
