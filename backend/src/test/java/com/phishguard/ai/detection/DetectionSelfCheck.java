package com.phishguard.ai.detection;

/** Dependency-free smoke check (run with plain java) used while Maven/JUnit are unavailable. */
public class DetectionSelfCheck {
    static int failures = 0;
    static final DetectionEngine E = new DetectionEngine();

    static void url(String in, Classification min, Classification max) {
        try {
            var r = E.analyzeUrl(in);
            var c = r.assessment().classification();
            boolean ok = c.ordinal() >= min.ordinal() && c.ordinal() <= max.ordinal();
            System.out.printf("%s URL %-52s -> %3d %s %s%n", ok ? "PASS" : "FAIL", in, r.assessment().score(), c,
                r.assessment().findings().stream().map(f -> f.finding().ruleId() + "+" + f.scoreContribution()).toList());
            if (!ok) failures++;
        } catch (InvalidInputException e) { System.out.println("FAIL URL " + in + " rejected: " + e.getMessage()); failures++; }
    }
    static void rejected(String in) {
        try { E.analyzeUrl(in); System.out.println("FAIL should reject: " + in); failures++; }
        catch (InvalidInputException e) { System.out.println("PASS rejected " + in + " (" + e.getMessage() + ")"); }
    }
    static void msg(String label, String in, Classification min, Classification max) {
        var r = E.analyzeMessage(in);
        var c = r.assessment().classification();
        boolean ok = c.ordinal() >= min.ordinal() && c.ordinal() <= max.ordinal();
        System.out.printf("%s MSG %-22s -> %3d %s %s urls=%s%n", ok ? "PASS" : "FAIL", label, r.assessment().score(), c,
            r.assessment().findings().stream().map(f -> f.finding().ruleId() + "+" + f.scoreContribution()).toList(), r.extractedUrls());
        if (!ok) failures++;
    }

    public static void main(String[] a) {
        var S = Classification.SAFE_LOOKING; var U = Classification.SUSPICIOUS; var H = Classification.HIGH_RISK;
        url("https://www.wikipedia.org", S, S);
        url("docs.google.com/document/d/abc", S, S);
        url("https://accounts.google.com/signin", S, S);
        url("http://192.168.0.1/login", U, H);
        url("https://paypal-security-login.xyz/verify", H, H);
        url("http://bit.ly/3Xk9a7", U, U);
        url("https://xn--pple-43d.com/", H, H);
        url("https://paypal.com@evil.example/signin", U, H);
        url("https://example.com:8443/a?next=https://evil.test", S, U);
        rejected("javascript:alert(1)"); rejected("ftp://files.example.com"); rejected("not a url"); rejected("");
        rejected("https://exa mple.com"); rejected("https://example.com:99999/");

        msg("phish", "URGENT: Your account has been suspended. Verify your password immediately at http://secure-paypa1-login.top/verify or it will be closed within 24 hours.", H, H);
        msg("prize", "Congratulations! You won a $1000 gift card. Claim your prize at bit.ly/3Xk9a7", U, H);
        msg("otp-scam", "This is your bank. Please share your OTP to avoid account blocked. Don't tell anyone.", H, H);
        msg("legit-appt", "Hi, your appointment is confirmed for Tuesday at 10:30. Reply STOP to opt out.", S, S);
        msg("legit-otp", "Your verification code is 482913. It expires in 10 minutes. Do not share this code with anyone.", S, S);
        msg("legit-bill", "Reminder: your electricity bill is due tomorrow. Please pay via the official app to avoid late fees.", S, S);
        try { E.analyzeMessage("x".repeat(10_001)); System.out.println("FAIL oversize accepted"); failures++; }
        catch (InvalidInputException e) { System.out.println("PASS oversize rejected"); }

        System.out.println(failures == 0 ? "\nALL CHECKS PASSED" : "\nFAILURES: " + failures);
        System.exit(failures == 0 ? 0 : 1);
    }
}
