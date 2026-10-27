package com.phishguard.ai.detection;

import java.util.*;

public final class UrlFeatureExtractor {
    static final Set<String> SHORTENERS = Set.of("bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd",
        "buff.ly", "rebrand.ly", "cutt.ly", "shorturl.at", "tiny.cc", "rb.gy");
    private static final List<String> KEYWORDS = List.of("login", "signin", "secure", "verify", "verification",
        "account", "update", "billing", "wallet", "password", "banking", "confirm", "support", "unlock");

    private UrlFeatureExtractor() {}

    public static UrlFeatures extract(NormalizedUrl u) {
        String host = u.host();
        String registered = u.hostIsIp() ? host : DomainUtil.registeredDomain(host);
        List<String> subs = u.hostIsIp() ? List.of() : DomainUtil.subdomainLabels(host, registered);
        String tld = u.hostIsIp() ? "" : host.substring(host.lastIndexOf('.') + 1);
        boolean official = !u.hostIsIp() && BrandCatalog.isOfficial(registered);

        int hyphens = 0, digits = 0;
        for (char c : host.replace("xn--", "").toCharArray()) { if (c == '-') hyphens++; if (Character.isDigit(c)) digits++; }
        String tail = u.path() + (u.query() == null ? "" : "?" + u.query());
        int pct = 0;
        for (int i = 0; i + 2 < tail.length(); i++) if (tail.charAt(i) == '%' && isHex(tail.charAt(i + 1)) && isHex(tail.charAt(i + 2))) pct++;

        boolean punycode = Arrays.stream(host.split("\\.")).anyMatch(l -> l.startsWith("xn--"));
        boolean mixed = !u.hostIsIp() && DomainUtil.mixedScript(u.unicodeHost());
        boolean shortener = SHORTENERS.contains(registered);
        boolean nonStd = u.port() != null && !((u.scheme().equals("http") && u.port() == 80) || (u.scheme().equals("https") && u.port() == 443));
        String lowerTail = tail.toLowerCase(Locale.ROOT);
        boolean embedded = lowerTail.contains("http://") || lowerTail.contains("https://") || lowerTail.contains("%2f%2f") || u.path().contains("//");

        List<String> hostKw = new ArrayList<>(), pathKw = new ArrayList<>();
        if (!official && !u.hostIsIp()) {
            Set<String> hostTokens = new HashSet<>(Arrays.asList(host.split("[.\\-_]")));
            for (String k : KEYWORDS) if (hostTokens.contains(k)) hostKw.add(k);
        }
        for (String k : KEYWORDS) if (lowerTail.contains(k)) pathKw.add(k);
        List<String> brands = u.hostIsIp() || official ? List.of() : BrandCatalog.impersonated(u.unicodeHost(), registered);

        return new UrlFeatures(u.normalized().length(), host, registered, tld, subs, hyphens, digits, pct,
            punycode, mixed, shortener, nonStd, embedded, hostKw, pathKw, brands, official, u.scheme().equals("http"));
    }

    private static boolean isHex(char c) { return Character.digit(c, 16) >= 0; }
}
