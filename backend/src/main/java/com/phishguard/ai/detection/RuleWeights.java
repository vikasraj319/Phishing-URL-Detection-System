package com.phishguard.ai.detection;

import java.util.HashMap;
import java.util.Map;

/** Per-rule base weights (0-100). Overridable via configuration; defaults are uncalibrated. */
public final class RuleWeights {
    private static final Map<String, Integer> DEFAULTS = Map.ofEntries(
        Map.entry("ip_host", 45), Map.entry("userinfo_in_url", 35),
        Map.entry("punycode_host", 25), Map.entry("mixed_script_host", 35),
        Map.entry("url_shortener", 30), Map.entry("brand_impersonation", 50),
        Map.entry("suspicious_keywords_host", 15), Map.entry("suspicious_keywords_path", 8),
        Map.entry("suspicious_tld", 12), Map.entry("excessive_hyphens", 10),
        Map.entry("many_subdomains", 12), Map.entry("long_url", 8),
        Map.entry("non_standard_port", 10), Map.entry("percent_encoding", 8),
        Map.entry("embedded_url", 15), Map.entry("plain_http", 5),
        Map.entry("urgency", 8), Map.entry("account_threat", 22),
        Map.entry("credential_request", 30), Map.entry("otp_request", 35),
        Map.entry("payment_request", 22), Map.entry("prize_scam", 30),
        Map.entry("impersonation_claim", 12), Map.entry("bypass_verification", 30),
        Map.entry("link_click_prompt", 6), Map.entry("urgency_with_sensitive_request", 18));

    private final Map<String, Integer> overrides = new HashMap<>();

    public RuleWeights() {}
    public RuleWeights(Map<String, Integer> overrides) {
        if (overrides != null) this.overrides.putAll(overrides);
    }

    public int weight(String ruleId) {
        Integer w = overrides.get(ruleId);
        if (w == null) w = DEFAULTS.get(ruleId);
        if (w == null) throw new IllegalArgumentException("Unknown rule: " + ruleId);
        return Math.max(0, Math.min(100, w));
    }
}
