package com.ragapp.model;

public record IngestResponse(boolean success, String message, int chunks) {}