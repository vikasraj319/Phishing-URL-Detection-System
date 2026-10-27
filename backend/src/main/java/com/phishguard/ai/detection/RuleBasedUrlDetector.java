package com.phishguard.ai.detection;

import java.util.*;

/** Deterministic, explainable URL rules. Every finding carries evidence and an explanation. */
public final class RuleBasedUrlDetector {
    private static final Set<String> RISKY_TLDS = Set.of("zip", "mov", "xyz", "top", "tk", "ml", "ga", "cf", "gq",
        "click", "country", "work", "icu", "buzz", "rest", "support", "monster");
    private final RuleWeights w;

    public RuleBasedUrlDetector(RuleWeights w) { this.w = w; }

    public List<Finding> evaluate(NormalizedUrl u, UrlFeatures f) {
        List<Finding> r = new ArrayList<>();
        if (u.hostIsIp()) r.add(Finding.of("ip_host", "IP address instead of a domain name", Severity.HIGH,
            "Legitimate services rarely ask people to sign in at a bare IP address; it hides who operates the site.",
            "Host: " + u.host(), w.weight("ip_host")));
        if (u.hasUserInfo()) r.add(Finding.of("userinfo_in_url", "Credentials-style prefix before the host", Severity.HIGH,
            "Text before '@' is ignored when connecting, so attackers use it to make a link appear to belong to another site.",
            "Prefix '" + u.userName() + "@' precedes the real host " + u.host(), w.weight("userinfo_in_url")));
        if (f.mixedScript()) r.add(Finding.of("mixed_script_host", "Mixed alphabets in the domain", Severity.HIGH,
            "Mixing Latin letters with look-alike letters from another alphabet is a common homograph trick.",
            "Host: " + u.unicodeHost(), w.weight("mixed_script_host")));
        else if (f.punycode()) r.add(Finding.of("punycode_host", "Internationalised (punycode) domain", Severity.MEDIUM,
            "Punycode domains can imitate well-known names with look-alike characters. Many are legitimate.",
            "Host: " + f.host(), w.weight("punycode_host")));
        if (f.shortener()) r.add(Finding.of("url_shortener", "Link shortener hides the destination", Severity.MEDIUM,
            "The final site cannot be seen from the link itself. Shorteners are used legitimately but also to mask phishing pages.",
            "Shortener: " + f.registeredDomain(), w.weight("url_shortener")));
        if (!f.impersonatedBrands().isEmpty()) r.add(Finding.of("brand_impersonation", "Brand name on an unrelated domain", Severity.HIGH,
            "The domain contains a well-known brand name but is not one of that brand's known domains.",
            "Brand: " + String.join(", ", f.impersonatedBrands()) + " | Registered domain: " + f.registeredDomain(),
            w.weight("brand_impersonation")));
        if (!f.hostKeywords().isEmpty()) r.add(Finding.of("suspicious_keywords_host", "Account-security words in the domain", Severity.MEDIUM,
            "Words like login or verify inside a domain name are used to make look-alike sites seem official.",
            "Keywords: " + String.join(", ", f.hostKeywords()), w.weight("suspicious_keywords_host")));
        if (!f.pathKeywords().isEmpty()) r.add(Finding.of("suspicious_keywords_path", "Credential-related words in the path", Severity.LOW,
            "Sign-in or verification wording in the path is common on real sites too, so this is only a weak signal.",
            "Keywords: " + String.join(", ", f.pathKeywords()), w.weight("suspicious_keywords_path")));
        if (RISKY_TLDS.contains(f.tld())) r.add(Finding.of("suspicious_tld", "Top-level domain often abused", Severity.LOW,
            "This ending is cheap and frequently used for throwaway sites. Plenty of legitimate sites use it, so it is not proof.",
            "TLD: ." + f.tld(), w.weight("suspicious_tld")));
        if (f.hyphenCountInHost() >= 3) r.add(Finding.of("excessive_hyphens", "Many hyphens in the domain", Severity.LOW,
            "Long hyphenated names are typical of domains built to resemble a brand plus a trust word.",
            f.hyphenCountInHost() + " hyphens in " + f.host(), w.weight("excessive_hyphens")));
        if (f.subdomainLabels().size() >= 4) r.add(Finding.of("many_subdomains", "Deeply nested subdomains", Severity.LOW,
            "Many subdomain levels can push the real domain out of view on small screens.",
            f.subdomainLabels().size() + " subdomain levels before " + f.registeredDomain(), w.weight("many_subdomains")));
        if (f.length() > 100) r.add(Finding.of("long_url", "Very long URL", Severity.INFO,
            "Long URLs can bury the real destination or carry tracking and redirect data.",
            f.length() + " characters", w.weight("long_url")));
        if (f.nonStandardPort()) r.add(Finding.of("non_standard_port", "Non-standard port", Severity.LOW,
            "Consumer web sites almost always use the default port.", "Port: " + u.port(), w.weight("non_standard_port")));
        if (f.percentEncodedCount() >= 3) r.add(Finding.of("percent_encoding", "Heavy percent-encoding", Severity.LOW,
            "Encoding many characters can hide wording or destinations from a quick glance.",
            f.percentEncodedCount() + " encoded characters", w.weight("percent_encoding")));
        if (f.embeddedUrl()) r.add(Finding.of("embedded_url", "Another URL embedded in the link", Severity.MEDIUM,
            "A second address inside the path or query can be an open-redirect or a disguise for the real target.",
            "Query/path contains a nested address", w.weight("embedded_url")));
        if (f.plainHttp()) r.add(Finding.of("plain_http", "Connection is not encrypted (http)", Severity.INFO,
            "Missing HTTPS is a weak signal on its own. Note that HTTPS alone does not make a site trustworthy.",
            "Scheme: http", w.weight("plain_http")));
        return r;
    }
}
