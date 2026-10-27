package com.phishguard.ai.service;

import static org.junit.jupiter.api.Assertions.*;

import com.phishguard.ai.config.AppProperties;
import com.phishguard.ai.detection.*;
import com.phishguard.ai.dto.ApiDtos.ProviderResultDto;
import com.phishguard.ai.intelligence.ProviderException;
import com.phishguard.ai.intelligence.ThreatIntelProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ThreatIntelligenceServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), ZoneOffset.UTC);
    private static final AppProperties PROPS = new AppProperties(30, new AppProperties.Detection(30, 60, null),
        new AppProperties.RateLimit(30, 300), new AppProperties.Cors(List.of()), new AppProperties.Intel(1000, 600, null, null));

    private static ThreatIntelProvider provider(boolean configured, AtomicInteger calls, Behavior b) {
        return new ThreatIntelProvider() {
            public String name() { return "Fake"; }
            public boolean configured() { return configured; }
            public ProviderResultDto check(String url) throws ProviderException { calls.incrementAndGet(); return b.run(); }
        };
    }
    interface Behavior { ProviderResultDto run() throws ProviderException; }

    @Test void failureIsUnavailableNeverClean() {
        var svc = new ThreatIntelligenceService(List.of(provider(true, new AtomicInteger(), () -> { throw new ProviderException("Provider quota exceeded."); })), PROPS, CLOCK);
        var r = svc.checkUrl("https://example.com/");
        assertEquals("UNAVAILABLE", r.get(0).status());
        assertEquals("UNAVAILABLE", svc.status().providers().get(0).status());
    }

    @Test void unexpectedRuntimeErrorsAreContained() {
        var svc = new ThreatIntelligenceService(List.of(provider(true, new AtomicInteger(), () -> { throw new IllegalStateException("boom"); })), PROPS, CLOCK);
        assertEquals("UNAVAILABLE", svc.checkUrl("https://example.com/").get(0).status());
    }

    @Test void unconfiguredProvidersAreSkippedAndReportedHonestly() {
        var calls = new AtomicInteger();
        var svc = new ThreatIntelligenceService(List.of(provider(false, calls, () -> null)), PROPS, CLOCK);
        assertTrue(svc.checkUrl("https://example.com/").isEmpty());
        assertEquals(0, calls.get());
        assertEquals("NOT_CONFIGURED", svc.status().providers().get(0).status());
    }

    @Test void successfulResultsAreCachedButFailuresAreNot() {
        var ok = new AtomicInteger();
        var svc = new ThreatIntelligenceService(List.of(provider(true, ok, () -> new ProviderResultDto("Fake", "CLEAN", "ok"))), PROPS, CLOCK);
        svc.checkUrl("https://example.com/"); svc.checkUrl("https://example.com/");
        assertEquals(1, ok.get());
        var bad = new AtomicInteger();
        var svc2 = new ThreatIntelligenceService(List.of(provider(true, bad, () -> { throw new ProviderException("down"); })), PROPS, CLOCK);
        svc2.checkUrl("https://example.com/"); svc2.checkUrl("https://example.com/");
        assertEquals(2, bad.get());
    }

    @Test void flaggedEvidenceRaisesScoreButCleanOrUnavailableNeverLowersIt() {
        var scoring = new RiskScoringService(Thresholds.defaults());
        var base = new DetectionEngine().analyzeUrl("https://www.wikipedia.org").assessment();
        var flagged = scoring.combine(base, List.of(new ProviderResultDto("Fake", "FLAGGED", "malware")));
        assertEquals(Classification.HIGH_RISK, flagged.classification());
        var risky = new DetectionEngine().analyzeUrl("https://paypal-security-login.xyz/verify").assessment();
        for (String s : List.of("CLEAN", "UNKNOWN", "UNAVAILABLE"))
            assertEquals(risky.score(), scoring.combine(risky, List.of(new ProviderResultDto("Fake", s, ""))).score());
    }
}
