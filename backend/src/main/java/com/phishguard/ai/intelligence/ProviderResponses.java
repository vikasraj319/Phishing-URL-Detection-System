package com.phishguard.ai.intelligence;

import com.phishguard.ai.dto.ApiDtos.ProviderResultDto;
import java.util.List;
import java.util.Map;

/**
 * Interprets provider replies. Pure Java on plain Maps, so it does not depend on the JSON library version.
 * Anything unexpected becomes a ProviderException (reported as UNAVAILABLE), never CLEAN.
 */
public final class ProviderResponses {
    private ProviderResponses() {}

    /** Safe Browsing v4: an empty object means no match; "matches" lists threats. */
    public static ProviderResultDto safeBrowsing(String name, Map<String, Object> resp) throws ProviderException {
        if (resp == null) throw new ProviderException("Empty response from provider.");
        Object matches = resp.get("matches");
        if (matches == null) {
            if (!resp.isEmpty()) throw new ProviderException("Unexpected response from provider.");
            return new ProviderResultDto(name, "CLEAN", "Not on Safe Browsing lists (this is not a guarantee of safety)");
        }
        if (matches instanceof List<?> l && !l.isEmpty()) {
            String type = "threat";
            if (l.get(0) instanceof Map<?, ?> m && m.get("threatType") instanceof String s && !s.isBlank()) type = s;
            return new ProviderResultDto(name, "FLAGGED", "Listed as " + type.toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
        }
        throw new ProviderException("Unexpected response from provider.");
    }

    /** VirusTotal v3 URL report. 404 = never analysed (UNKNOWN), 429 = quota, other non-200 = failure. */
    public static ProviderResultDto virusTotal(String name, int httpStatus, Map<String, Object> body) throws ProviderException {
        if (httpStatus == 404) return new ProviderResultDto(name, "UNKNOWN", "No existing report for this URL");
        if (httpStatus == 429) throw new ProviderException("Provider quota exceeded.");
        if (httpStatus != 200 || body == null) throw new ProviderException("Provider returned HTTP " + httpStatus + ".");
        Map<String, Object> stats = dig(body, "data", "attributes", "last_analysis_stats");
        if (stats == null) return new ProviderResultDto(name, "UNKNOWN", "Report had no analysis statistics");
        int malicious = num(stats.get("malicious")), suspicious = num(stats.get("suspicious"));
        if (malicious > 0 || suspicious > 0)
            return new ProviderResultDto(name, "FLAGGED", malicious + " engines malicious, " + suspicious + " suspicious");
        return new ProviderResultDto(name, "CLEAN", "No engines flagged this URL in the latest report (not a guarantee of safety)");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dig(Map<String, Object> root, String... path) {
        Object cur = root;
        for (String p : path) {
            if (!(cur instanceof Map<?, ?> m)) return null;
            cur = m.get(p);
        }
        return cur instanceof Map<?, ?> ? (Map<String, Object>) cur : null;
    }

    private static int num(Object o) { return o instanceof Number n ? n.intValue() : 0; }
}
