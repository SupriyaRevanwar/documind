package com.ragapp.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final SimpleVectorStore vectorStore;
    private final DocumentSplitter documentSplitter;

    @Value("${vectorstore.path:./vectorstore.json}")
    private String vectorStorePath;

    public int ingest(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        log.info("Ingesting file: {} ({})", filename, contentType);

        List<org.springframework.ai.document.Document> springAiDocs;

        if (contentType.equals("application/pdf") || filename.endsWith(".pdf")) {
            springAiDocs = ingestPdf(file);
        } else {
            springAiDocs = ingestWithTika(file, filename);
        }

        vectorStore.add(springAiDocs);

        // Persist to disk so vectors survive app restarts
        vectorStore.save(new File(vectorStorePath));
        log.info("Stored {} chunks from '{}' into vector store", springAiDocs.size(), filename);
        return springAiDocs.size();
    }

    private List<org.springframework.ai.document.Document> ingestPdf(MultipartFile file) throws Exception {
        Resource resource = file.getResource();
        List<org.springframework.ai.document.Document> pages = new PagePdfDocumentReader(resource).get();
        log.debug("PDF has {} pages, splitting into chunks...", pages.size());

        return pages.stream()
                .flatMap(page -> {
                    Document lc4jDoc = Document.from(
                            page.getContent(),
                            dev.langchain4j.data.document.Metadata.from(
                                    "source", page.getMetadata().getOrDefault("source", "pdf").toString())
                    );
                    return toSpringAiDocs(documentSplitter.split(lc4jDoc),
                            page.getMetadata()).stream();
                })
                .toList();
    }

    private List<org.springframework.ai.document.Document> ingestWithTika(
            MultipartFile file, String filename) throws Exception {

        File tempFile = File.createTempFile("upload-", "-" + filename);
        file.transferTo(tempFile);

        ApacheTikaDocumentParser tikaParser = new ApacheTikaDocumentParser();
        Document lc4jDoc = FileSystemDocumentLoader.loadDocument(tempFile.toPath(), tikaParser);
        lc4jDoc.metadata().put("source", filename);
        tempFile.deleteOnExit();

        List<TextSegment> segments = documentSplitter.split(lc4jDoc);
        log.debug("Tika split '{}' into {} segments", filename, segments.size());
        return toSpringAiDocs(segments, Map.of("source", filename));
    }

    private List<org.springframework.ai.document.Document> toSpringAiDocs(
            List<TextSegment> segments, Map<String, Object> extraMeta) {
        return segments.stream()
                .map(segment -> {
                    Map<String, Object> meta = new HashMap<>(extraMeta);
                    segment.metadata().toMap().forEach(meta::put);
                    return new org.springframework.ai.document.Document(segment.text(), meta);
                })
                .toList();
    }
}