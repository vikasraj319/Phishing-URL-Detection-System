package com.phishguard.ai.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** All tunables. Defaults are uncalibrated starting points; override via environment variables. */
@ConfigurationProperties(prefix = "phishguard")
public record AppProperties(
    /** Days to keep scans; 0 keeps them until deleted by the user. */
    @DefaultValue("30") int retentionDays,
    @DefaultValue Detection detection,
    @DefaultValue RateLimit rateLimit,
    @DefaultValue Cors cors,
    @DefaultValue Intel intel) {

    public record Detection(@DefaultValue("30") int suspiciousMin, @DefaultValue("60") int highRiskMin, Map<String, Integer> ruleWeights) {}
    public record RateLimit(@DefaultValue("30") int scansPerMinute, @DefaultValue("300") int readsPerMinute) {}
    public record Cors(@DefaultValue("http://localhost:5173") List<String> allowedOrigins) {}
    public record Intel(@DefaultValue("3000") int timeoutMs, @DefaultValue("600") int cacheSeconds,
                        String safeBrowsingApiKey, String virusTotalApiKey) {}
}
