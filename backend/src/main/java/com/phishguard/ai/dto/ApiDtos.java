package com.phishguard.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Wire format shared with the React client (see frontend/src/lib/types.ts). */
public final class ApiDtos {
    private ApiDtos() {}

    public record SpanDto(int start, int end) {}

    public record FindingDto(String ruleId, String title, String severity, String explanation, String evidence,
                             int scoreContribution, List<SpanDto> spans) {}

    /** status: CLEAN | FLAGGED | UNKNOWN | UNAVAILABLE | NOT_CONFIGURED. UNAVAILABLE is never a clean result. */
    public record ProviderResultDto(String provider, String status, String detail) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ScanResultDto(UUID id, String type, String inputDisplay, int score, String classification,
                                List<FindingDto> findings, List<ProviderResultDto> providers,
                                List<String> extractedUrls, Instant createdAt, String messageText) {}

    public record ScanListItemDto(UUID id, String type, String inputDisplay, int score, String classification, Instant createdAt) {}

    public record PageDto<T>(List<T> items, int page, int size, long totalItems, int totalPages) {}

    public record PeriodCountsDto(long total, long highRisk, long suspicious) {}

    public record DashboardStatsDto(long totalScans, long highRisk, long suspicious, long safeLooking,
                                    Double flaggedRate, PeriodCountsDto previous) {}

    public record RiskDistributionDto(long total, long safeLooking, long suspicious, long highRisk) {}

    public record IndicatorCountDto(String ruleId, String title, long count) {}

    public record ProviderStatusDto(String name, boolean configured, String status, Instant lastSuccessAt, String detail) {}

    public record ThreatIntelStatusDto(List<ProviderStatusDto> providers) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApiErrorDto(int status, String code, String message, Map<String, String> fieldErrors) {}
}
