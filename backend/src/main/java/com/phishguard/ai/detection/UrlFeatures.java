package com.phishguard.ai.detection;

import java.util.List;

public record UrlFeatures(int length, String host, String registeredDomain, String tld,
                          List<String> subdomainLabels, int hyphenCountInHost, int digitCountInHost,
                          int percentEncodedCount, boolean punycode, boolean mixedScript, boolean shortener,
                          boolean nonStandardPort, boolean embeddedUrl, List<String> hostKeywords,
                          List<String> pathKeywords, List<String> impersonatedBrands, boolean officialDomain,
                          boolean plainHttp) {}
