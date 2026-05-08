package com.ragapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final SimpleVectorStore vectorStore;

    @Value("${rag.top-k:5}")
    private int topK;

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions based on the provided document context.
            Guidelines:
            - Answer ONLY from the context provided below.
            - If the answer is not in the context, say "I don't have information about that in the uploaded documents."
            - Be concise and cite which part of the document supports your answer.
            - Do not make up information.
            """;

    public String chat(String question, String sessionId) {
        log.debug("RAG chat | session={} | question={}", sessionId, question);

        String answer = chatClientBuilder
                .build()
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(question)
                .advisors(new QuestionAnswerAdvisor(
                        vectorStore,
                        SearchRequest.defaults().withTopK(topK)
                ))
                .call()
                .content();

        log.debug("RAG answer | session={} | length={}", sessionId, answer.length());
        return answer;
    }
}