package com.ragapp.model;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "question must not be blank")
        String question,
        String sessionId
) {}