package com.phishguard.ai.detection;

import com.phishguard.ai.intelligence.*;
import java.util.*;

/** Plain-Java check of provider reply interpretation (run on any JDK). */
public class ProviderResponsesSelfCheck {
    static int bad = 0;
    static void eq(String n, Object got, Object want) { boolean ok = Objects.equals(got, want); System.out.println((ok ? "PASS " : "FAIL ") + n + (ok ? "" : " got=" + got + " want=" + want)); if (!ok) bad++; }
    static String sb(Map<String, Object> m) { try { return ProviderResponses.safeBrowsing("SB", m).status(); } catch (ProviderException e) { return "UNAVAILABLE"; } }
    static String vt(int s, Map<String, Object> m) { try { return ProviderResponses.virusTotal("VT", s, m).status(); } catch (ProviderException e) { return "UNAVAILABLE"; } }
    public static void main(String[] a) {
        eq("SB empty object = clean", sb(Map.of()), "CLEAN");
        eq("SB null = unavailable", sb(null), "UNAVAILABLE");
        eq("SB match = flagged", sb(Map.of("matches", List.of(Map.of("threatType", "SOCIAL_ENGINEERING")))), "FLAGGED");
        eq("SB threat label", ProviderResponses.class.getSimpleName() != null ? safeLabel() : "", "Listed as social engineering");
        eq("SB unexpected shape = unavailable", sb(Map.of("error", Map.of("code", 403))), "UNAVAILABLE");
        eq("SB empty matches list = unavailable", sb(Map.of("matches", List.of())), "UNAVAILABLE");
        eq("VT 404 = unknown", vt(404, null), "UNKNOWN");
        eq("VT 429 = unavailable", vt(429, null), "UNAVAILABLE");
        eq("VT 500 = unavailable", vt(500, null), "UNAVAILABLE");
        eq("VT 200 no body = unavailable", vt(200, null), "UNAVAILABLE");
        eq("VT no stats = unknown", vt(200, Map.of("data", Map.of())), "UNKNOWN");
        eq("VT malicious = flagged", vt(200, Map.of("data", Map.of("attributes", Map.of("last_analysis_stats", Map.of("malicious", 3, "suspicious", 0))))), "FLAGGED");
        eq("VT zeroes = clean", vt(200, Map.of("data", Map.of("attributes", Map.of("last_analysis_stats", Map.of("malicious", 0, "suspicious", 0, "harmless", 70))))), "CLEAN");
        System.out.println(bad == 0 ? "ALL PROVIDER CHECKS PASSED" : "FAILURES " + bad);
        System.exit(bad == 0 ? 0 : 1);
    }
    static String safeLabel() { try { return ProviderResponses.safeBrowsing("SB", Map.of("matches", List.of(Map.of("threatType", "SOCIAL_ENGINEERING")))).detail(); } catch (ProviderException e) { return "ERR"; } }
}
