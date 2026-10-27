package com.phishguard.ai.detection;

import java.util.*;

/**
 * Aggregation: each distinct rule counts once (strongest instance wins), then weights are
 * combined as a noisy-OR: score = 100 * (1 - prod(1 - w_i/100)). This keeps the result in
 * 0..100, rewards independent corroborating signals, and stops many weak or duplicate
 * signals from inflating the score. Per-finding contributions are the marginal increase in
 * the rounded score as findings are added in descending weight order, so they sum exactly
 * to the total. This is a heuristic score, not a calibrated probability.
 */
public final class RiskScorer {
    private final Thresholds thresholds;

    public RiskScorer(Thresholds thresholds) { this.thresholds = thresholds; }

    public RiskAssessment assess(List<Finding> raw) {
        Map<String, Finding> byRule = new LinkedHashMap<>();
        for (Finding f : raw) byRule.merge(f.ruleId(), f, (a, b) -> a.weight() >= b.weight() ? a : b);
        List<Finding> sorted = new ArrayList<>(byRule.values());
        sorted.sort(Comparator.comparingInt(Finding::weight).reversed());

        double remaining = 1.0;
        int previous = 0;
        List<ScoredFinding> out = new ArrayList<>();
        for (Finding f : sorted) {
            remaining *= 1.0 - f.weight() / 100.0;
            int cumulative = (int) Math.round(100.0 * (1.0 - remaining));
            out.add(new ScoredFinding(f, cumulative - previous));
            previous = cumulative;
        }
        return new RiskAssessment(previous, thresholds.classify(previous), List.copyOf(out));
    }
}
