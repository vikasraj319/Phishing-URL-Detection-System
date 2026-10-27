package com.phishguard.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.phishguard.ai.detection.InvalidInputException;
import com.phishguard.ai.dto.ApiDtos.ScanResultDto;
import com.phishguard.ai.exception.GlobalExceptionHandler;
import com.phishguard.ai.exception.NotFoundException;
import com.phishguard.ai.service.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ScanControllerTest {
    private final UrlAnalysisService urls = mock(UrlAnalysisService.class);
    private final MessageAnalysisService messages = mock(MessageAnalysisService.class);
    private final ScanHistoryService history = mock(ScanHistoryService.class);
    private MockMvc mvc;

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ScanController(urls, messages, history)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private static ScanResultDto sample(UUID id) {
        return new ScanResultDto(id, "URL", "example.com", 12, "SAFE_LOOKING", List.of(), List.of(), List.of(), Instant.parse("2026-10-24T10:00:00Z"), null);
    }

    @Test void blankUrlIsRejectedWithFieldError() throws Exception {
        mvc.perform(post("/api/v1/scan/url").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"  \"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED")).andExpect(jsonPath("$.fieldErrors.url").exists());
        verifyNoInteractions(urls);
    }

    @Test void oversizedMessageIsRejected() throws Exception {
        String body = "{\"message\":\"" + "a".repeat(10_001) + "\"}";
        mvc.perform(post("/api/v1/scan/message").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(messages);
    }

    @Test void engineRejectionBecomesBadRequest() throws Exception {
        when(urls.scan(any())).thenThrow(new InvalidInputException("Only http and https URLs can be analyzed."));
        mvc.perform(post("/api/v1/scan/url").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"ftp://x.test\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test void validScanReturnsTheAssessment() throws Exception {
        when(urls.scan("https://example.com")).thenReturn(sample(UUID.randomUUID()));
        mvc.perform(post("/api/v1/scan/url").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://example.com\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.score").value(12)).andExpect(jsonPath("$.classification").value("SAFE_LOOKING"));
    }

    @Test void missingScanIs404AndDeleteIs204() throws Exception {
        UUID id = UUID.randomUUID();
        when(history.get(id)).thenThrow(new NotFoundException("Scan not found."));
        mvc.perform(get("/api/v1/scans/" + id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(delete("/api/v1/scans/" + id)).andExpect(status().isNoContent());
        verify(history).delete(id);
    }

    @Test void wrongMethodIs405NotAServerError() throws Exception {
        mvc.perform(get("/api/v1/scan/url")).andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("HTTP_405"));
    }

    @Test void unexpectedErrorsDoNotLeakDetails() throws Exception {
        when(urls.scan(any())).thenThrow(new IllegalStateException("jdbc:postgresql://secret-host password=hunter2"));
        mvc.perform(post("/api/v1/scan/url").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://example.com\"}"))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").value("Something went wrong on the server."));
    }
}
