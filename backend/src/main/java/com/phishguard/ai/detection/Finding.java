package com.phishguard.ai.detection;

import java.util.List;

/**
 * One piece of evidence. {@code weight} is the raw strength (0-100) of the signal before
 * aggregation; the contribution to the final score is computed by {@link RiskScorer}.
 */
public record Finding(String ruleId, String title, Severity severity, String explanation,
                      String evidence, int weight, List<Span> spans) {
    public Finding {
        spans = spans == null ? List.of() : List.copyOf(spans);
    }
    public static Finding of(String ruleId, String title, Severity sev, String explanation,
                             String evidence, int weight) {
        return new Finding(ruleId, title, sev, explanation, evidence, weight, List.of());
    }
}
