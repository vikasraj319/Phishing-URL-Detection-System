package com.phishguard.ai.detection;

import java.util.*;

/**
 * Approximate registered-domain logic without the Public Suffix List. Handles common
 * multi-part suffixes only; a production build should swap in the full PSL.
 */
public final class DomainUtil {
    private static final Set<String> MULTI = Set.of("co.uk", "org.uk", "ac.uk", "gov.uk", "com.au", "net.au",
        "org.au", "co.in", "net.in", "org.in", "gov.in", "ac.in", "co.jp", "com.br", "com.cn", "co.nz",
        "co.za", "com.mx", "com.sg", "com.tr");
    private static final Map<Character, Character> CONFUSABLES = Map.ofEntries(
        Map.entry('а', 'a'), Map.entry('е', 'e'), Map.entry('о', 'o'), Map.entry('р', 'p'), Map.entry('с', 'c'),
        Map.entry('х', 'x'), Map.entry('у', 'y'), Map.entry('і', 'i'), Map.entry('ѕ', 's'), Map.entry('ј', 'j'),
        Map.entry('һ', 'h'), Map.entry('ԁ', 'd'), Map.entry('ӏ', 'l'), Map.entry('ı', 'i'), Map.entry('ο', 'o'),
        Map.entry('ν', 'v'), Map.entry('α', 'a'), Map.entry('ɡ', 'g'));

    private DomainUtil() {}

    public static String registeredDomain(String host) {
        String[] l = host.split("\\.");
        if (l.length <= 2) return host;
        String lastTwo = l[l.length - 2] + "." + l[l.length - 1];
        int keep = MULTI.contains(lastTwo) ? 3 : 2;
        return String.join(".", Arrays.copyOfRange(l, l.length - keep, l.length));
    }

    public static List<String> subdomainLabels(String host, String registered) {
        if (host.equals(registered) || !host.endsWith("." + registered)) return List.of();
        String sub = host.substring(0, host.length() - registered.length() - 1);
        return List.of(sub.split("\\."));
    }

    /** Maps visually confusable non-Latin letters to Latin look-alikes. */
    public static String skeleton(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) sb.append(CONFUSABLES.getOrDefault(c, c));
        return sb.toString();
    }

    public static boolean mixedScript(String unicodeHost) {
        Set<Character.UnicodeScript> scripts = new HashSet<>();
        unicodeHost.codePoints().filter(Character::isLetter).forEach(cp -> {
            Character.UnicodeScript sc = Character.UnicodeScript.of(cp);
            if (sc != Character.UnicodeScript.COMMON && sc != Character.UnicodeScript.INHERITED) scripts.add(sc);
        });
        return scripts.size() > 1 && scripts.contains(Character.UnicodeScript.LATIN);
    }
}
