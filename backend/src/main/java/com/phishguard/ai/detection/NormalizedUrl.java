package com.phishguard.ai.detection;

/** Parsed, validated URL. Credentials are never retained: only the user name is kept. */
public record NormalizedUrl(String original, String normalized, String scheme, boolean schemeAssumed,
                            String host, String unicodeHost, boolean hostIsIp, Integer port,
                            boolean hasUserInfo, String userName, String path, String query) {}
