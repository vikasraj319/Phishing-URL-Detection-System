package com.phishguard.ai.detection;

import java.util.*;

/** Small built-in brand list: brand token -> official registered domains. Extend via config later. */
public final class BrandCatalog {
    private static final Map<String, Set<String>> BRANDS = new LinkedHashMap<>();
    static {
        put("paypal", "paypal.com"); put("google", "google.com", "gmail.com", "youtube.com", "goo.gl");
        put("microsoft", "microsoft.com", "live.com", "office.com", "outlook.com", "microsoftonline.com");
        put("apple", "apple.com", "icloud.com"); put("amazon", "amazon.com", "amazon.in", "amazon.co.uk", "amazonaws.com");
        put("netflix", "netflix.com"); put("facebook", "facebook.com", "fb.com"); put("instagram", "instagram.com");
        put("whatsapp", "whatsapp.com", "whatsapp.net"); put("linkedin", "linkedin.com"); put("dropbox", "dropbox.com");
        put("docusign", "docusign.com", "docusign.net"); put("chase", "chase.com"); put("wellsfargo", "wellsfargo.com");
        put("hdfcbank", "hdfcbank.com"); put("icicibank", "icicibank.com"); put("sbi", "sbi.co.in");
        put("paytm", "paytm.com"); put("fedex", "fedex.com"); put("dhl", "dhl.com"); put("irs", "irs.gov");
    }
    private static void put(String brand, String... domains) { BRANDS.put(brand, Set.of(domains)); }

    private BrandCatalog() {}

    public static boolean isOfficial(String registeredDomain) {
        return BRANDS.values().stream().anyMatch(s -> s.contains(registeredDomain));
    }

    /** Brands whose name appears in the host labels although the domain is not theirs. */
    public static List<String> impersonated(String unicodeHost, String registeredDomain) {
        String skel = DomainUtil.skeleton(unicodeHost);
        String leet = skel.replace('1', 'l').replace('0', 'o').replace('3', 'e');
        Set<String> tokens = new HashSet<>();
        for (String v : List.of(skel, leet)) tokens.addAll(Arrays.asList(v.split("[.\\-_]")));
        List<String> hits = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : BRANDS.entrySet()) {
            String b = e.getKey();
            if (e.getValue().contains(registeredDomain)) continue;
            boolean match = tokens.stream().anyMatch(t -> t.equals(b) || (b.length() >= 6 && t.contains(b)));
            if (match) hits.add(b);
        }
        return hits;
    }
}
