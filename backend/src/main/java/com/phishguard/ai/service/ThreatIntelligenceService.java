package com.phishguard.ai.service;

import com.phishguard.ai.config.AppProperties;
import com.phishguard.ai.dto.ApiDtos.*;
import com.phishguard.ai.intelligence.ProviderException;
import com.phishguard.ai.intelligence.ThreatIntelProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs configured providers. A failure, timeout or quota error becomes UNAVAILABLE, which callers must never
 * read as "clean". Results other than UNAVAILABLE are cached by a hash of the URL (the URL itself is not retained).
 */
@Service
public class ThreatIntelligenceService {
    private static final Logger log = LoggerFactory.getLogger(ThreatIntelligenceService.class);
    private record Cached(ProviderResultDto result, Instant at) {}

    private final List<ThreatIntelProvider> providers;
    private final AppProperties props;
    private final Clock clock;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastSuccess = new ConcurrentHashMap<>();
    private final Map<String, String> lastFailure = new ConcurrentHashMap<>();

    public ThreatIntelligenceService(List<ThreatIntelProvider> providers, AppProperties props, Clock clock) {
        this.providers = providers; this.props = props; this.clock = clock;
    }

    /** Results from configured providers only; empty when none are configured. */
    public List<ProviderResultDto> checkUrl(String url) {
        List<ProviderResultDto> out = new ArrayList<>();
        for (ThreatIntelProvider p : providers) {
            if (!p.configured()) continue;
            out.add(checkOne(p, url));
        }
        return out;
    }

    /** Merges per-URL results per provider; the worst status wins (FLAGGED > UNAVAILABLE > UNKNOWN > CLEAN). */
    public List<ProviderResultDto> checkUrls(List<String> urls) {
        Map<String, ProviderResultDto> worst = new LinkedHashMap<>();
        for (String u : urls.stream().limit(5).toList()) {
            for (ProviderResultDto r : checkUrl(u)) worst.merge(r.provider(), r, (a, b) -> rank(b.status()) > rank(a.status()) ? b : a);
        }
        return List.copyOf(worst.values());
    }

    private static int rank(String s) {
        return switch (s) { case "FLAGGED" -> 4; case "UNAVAILABLE" -> 3; case "UNKNOWN" -> 2; default -> 1; };
    }

    private ProviderResultDto checkOne(ThreatIntelProvider p, String url) {
        String key = p.name() + ":" + sha256(url);
        Cached c = cache.get(key);
        if (c != null && c.at().plusSeconds(props.intel().cacheSeconds()).isAfter(clock.instant())) return c.result();
        try {
            ProviderResultDto r = p.check(url);
            lastSuccess.put(p.name(), clock.instant());
            lastFailure.remove(p.name());
            if (cache.size() > 2000) cache.clear();
            cache.put(key, new Cached(r, clock.instant()));
            return r;
        } catch (ProviderException e) {
            lastFailure.put(p.name(), e.getMessage());
            log.warn("Threat-intel provider {} unavailable: {}", p.name(), e.getMessage());
            return new ProviderResultDto(p.name(), "UNAVAILABLE", e.getMessage());
        } catch (RuntimeException e) {
            lastFailure.put(p.name(), "Unexpected provider error.");
            log.warn("Threat-intel provider {} failed ({})", p.name(), e.getClass().getSimpleName());
            return new ProviderResultDto(p.name(), "UNAVAILABLE", "Unexpected provider error.");
        }
    }

    /** CLEAN here means "last lookup worked"; the UI labels it Connected. */
    public ThreatIntelStatusDto status() {
        List<ProviderStatusDto> list = providers.stream().map(p -> {
            if (!p.configured()) return new ProviderStatusDto(p.name(), false, "NOT_CONFIGURED", null, "No API key is set on the server.");
            Instant ok = lastSuccess.get(p.name());
            String fail = lastFailure.get(p.name());
            String status = fail != null ? "UNAVAILABLE" : ok != null ? "CLEAN" : "UNKNOWN";
            String detail = fail != null ? "Last check failed: " + fail : ok == null ? "Configured; no lookups have been made yet." : null;
            return new ProviderStatusDto(p.name(), true, status, ok, detail);
        }).toList();
        return new ThreatIntelStatusDto(list);
    }

    static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
