package com.phishguard.ai.detection;

import java.net.IDN;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates and normalises untrusted URL text. Never resolves DNS or opens a connection. */
public final class UrlNormalizer {
    public static final int MAX_LENGTH = 2048;
    private static final Pattern URL = Pattern.compile(
        "^([a-zA-Z][a-zA-Z0-9+.-]*)://([^/?#]*)([^?#]*)(?:\\?([^#]*))?(?:#.*)?$");
    private static final Pattern DOTTED = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final Pattern LABEL = Pattern.compile("^[a-z0-9_]([a-z0-9_-]{0,61}[a-z0-9_])?$");
    private static final Pattern BLOCKED = Pattern.compile(
        "^(javascript|data|vbscript|file|mailto|about|blob):.*", Pattern.CASE_INSENSITIVE);

    private UrlNormalizer() {}

    public static NormalizedUrl normalize(String input) {
        if (input == null || input.isBlank()) throw new InvalidInputException("Enter a URL to analyze.");
        String raw = input.strip();
        if (raw.length() > MAX_LENGTH) throw new InvalidInputException("URL is longer than " + MAX_LENGTH + " characters.");
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isWhitespace(c) || Character.isISOControl(c)) {
                throw new InvalidInputException("URL must not contain spaces or control characters.");
            }
        }
        if (BLOCKED.matcher(raw).matches()) throw new InvalidInputException("Only http and https URLs can be analyzed.");

        boolean assumed = !raw.contains("://");
        String candidate = assumed ? "https://" + raw : raw;
        Matcher m = URL.matcher(candidate);
        if (!m.matches()) throw new InvalidInputException("This does not look like a valid URL.");
        String scheme = m.group(1).toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new InvalidInputException("Only http and https URLs can be analyzed.");
        }

        String authority = m.group(2);
        int at = authority.lastIndexOf('@');
        String userInfo = at >= 0 ? authority.substring(0, at) : null;
        String hostPort = at >= 0 ? authority.substring(at + 1) : authority;

        String host;
        Integer port = null;
        boolean ipv6 = false;
        if (hostPort.startsWith("[")) {
            int end = hostPort.indexOf(']');
            if (end < 0) throw new InvalidInputException("Invalid IPv6 host.");
            host = hostPort.substring(1, end).toLowerCase(Locale.ROOT);
            ipv6 = true;
            String rest = hostPort.substring(end + 1);
            if (!rest.isEmpty()) port = parsePort(rest.startsWith(":") ? rest.substring(1) : "x");
        } else {
            int colon = hostPort.lastIndexOf(':');
            if (colon >= 0) {
                port = parsePort(hostPort.substring(colon + 1));
                host = hostPort.substring(0, colon);
            } else {
                host = hostPort;
            }
        }
        if (host.isEmpty()) throw new InvalidInputException("URL has no host name.");
        if (host.endsWith(".")) host = host.substring(0, host.length() - 1);

        String unicodeHost = host.toLowerCase(Locale.ROOT);
        String asciiHost;
        boolean isIp = ipv6;
        if (ipv6) {
            asciiHost = host;
        } else {
            try {
                asciiHost = IDN.toASCII(unicodeHost, IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.ROOT);
            } catch (IllegalArgumentException e) {
                throw new InvalidInputException("Host name is not valid.");
            }
            if (asciiHost.length() > 253) throw new InvalidInputException("Host name is too long.");
            for (String label : asciiHost.split("\\.", -1)) {
                if (!LABEL.matcher(label).matches()) throw new InvalidInputException("Host name is not valid.");
            }
            isIp = looksLikeIp(asciiHost);
            try { unicodeHost = IDN.toUnicode(asciiHost).toLowerCase(Locale.ROOT); }
            catch (IllegalArgumentException ignored) { unicodeHost = asciiHost; }
        }

        String path = m.group(3) == null || m.group(3).isEmpty() ? "/" : m.group(3);
        String query = m.group(4);
        boolean defaultPort = port == null || (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443);
        String normalized = scheme + "://" + (ipv6 ? "[" + asciiHost + "]" : asciiHost)
            + (defaultPort ? "" : ":" + port) + path + (query != null ? "?" + query : "");
        String userName = userInfo == null ? null : userInfo.split(":", 2)[0];
        return new NormalizedUrl(input.strip(), normalized, scheme, assumed, asciiHost, unicodeHost, isIp,
            port, userInfo != null, userName, path, query);
    }

    private static int parsePort(String s) {
        if (!s.matches("\\d{1,5}")) throw new InvalidInputException("Port is not valid.");
        int p = Integer.parseInt(s);
        if (p < 1 || p > 65535) throw new InvalidInputException("Port is not valid.");
        return p;
    }

    static boolean looksLikeIp(String host) {
        Matcher d = DOTTED.matcher(host);
        if (d.matches()) {
            for (int i = 1; i <= 4; i++) if (Integer.parseInt(d.group(i)) > 255) return false;
            return true;
        }
        if (host.matches("\\d{8,10}")) return Long.parseLong(host) <= 4294967295L;
        return host.matches("0x[0-9a-f]{1,8}");
    }
}
