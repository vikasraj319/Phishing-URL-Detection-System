package com.phishguard.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ScanRequests {
    private ScanRequests() {}

    public record UrlScanRequest(
        @NotBlank(message = "Enter a URL to analyze.")
        @Size(max = 2048, message = "URL must be 2048 characters or fewer.") String url) {}

    /** allowExternalLookup is explicit consent to send links found in the message to reputation providers. */
    public record MessageScanRequest(
        @NotBlank(message = "Enter a message to analyze.")
        @Size(max = 10000, message = "Message must be 10,000 characters or fewer.") String message,
        boolean allowExternalLookup) {}
}
