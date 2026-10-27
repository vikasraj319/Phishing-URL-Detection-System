package com.phishguard.ai.detection;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class RiskScorerTest {
    private final RiskScorer scorer = new RiskScorer(Thresholds.defaults());
    private static Finding f(String id, int w) { return Finding.of(id, id, Severity.LOW, "x", "e", w); }

    @Test void emptyIsZero() { assertEquals(0, scorer.assess(List.of()).score()); }

    @Test void duplicateRulesCountOnce() { assertEquals(scorer.assess(List.of(f("a", 30))).score(), scorer.assess(List.of(f("a", 30), f("a", 20))).score()); }

    @Test void scoreIsBoundedAndContributionsSumToTotal() {
        var a = scorer.assess(List.of(f("a", 90), f("b", 90), f("c", 90), f("d", 90)));
        assertTrue(a.score() <= 100);
        assertEquals(a.score(), a.findings().stream().mapToInt(ScoredFinding::scoreContribution).sum());
    }

    @Test void manyWeakSignalsDoNotInflateToHighRisk() {
        var a = scorer.assess(List.of(f("a", 8), f("b", 5), f("c", 8), f("d", 10)));
        assertNotEquals(Classification.HIGH_RISK, a.classification());
    }

    @Test void thresholdsClassifyAtBoundaries() {
        var t = Thresholds.defaults();
        assertEquals(Classification.SAFE_LOOKING, t.classify(29));
        assertEquals(Classification.SUSPICIOUS, t.classify(30));
        assertEquals(Classification.SUSPICIOUS, t.classify(59));
        assertEquals(Classification.HIGH_RISK, t.classify(60));
        assertThrows(IllegalArgumentException.class, () -> new Thresholds(60, 30));
    }
}
