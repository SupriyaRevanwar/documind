package com.ragapp.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.File;

/**
 * RagConfig — uses SimpleVectorStore instead of Chroma.
 *
 * SimpleVectorStore:
 *  - Pure Java, no external dependencies, no Docker, no HTTP calls
 *  - Stores vectors in memory, persists to a JSON file on disk
 *  - Loads existing vectors from JSON on startup (survives restarts)
 *  - Similarity search uses cosine similarity in-memory
 *  - Perfect for development and moderate document collections (<100k chunks)
 *
 * For production scale → swap back to Chroma/Pinecone/pgvector by just
 * changing this bean. All other code stays identical.
 */
@Configuration
public class RagConfig {

    @Value("${rag.chunk-size:500}")
    private int chunkSize;

    @Value("${rag.chunk-overlap:50}")
    private int chunkOverlap;

    @Value("${vectorstore.path:./vectorstore.json}")
    private String vectorStorePath;

    @Bean
    public DocumentSplitter documentSplitter() {
        return DocumentSplitters.recursive(chunkSize, chunkOverlap);
    }

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = new SimpleVectorStore(embeddingModel);

        // Load existing vectors from disk if file exists (persist between restarts)
        File storeFile = new File(vectorStorePath);
        if (storeFile.exists()) {
            store.load(new FileSystemResource(storeFile));
            System.out.println("[RagConfig] Loaded existing vector store from: " + vectorStorePath);
        } else {
            System.out.println("[RagConfig] Starting fresh vector store (no existing file found).");
        }

        return store;
    }
}