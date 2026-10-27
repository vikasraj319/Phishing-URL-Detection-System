package com.phishguard.ai.detection;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DetectionEngineTest {
    private final DetectionEngine engine = new DetectionEngine();

    private Classification url(String s) { return engine.analyzeUrl(s).assessment().classification(); }
    private Classification msg(String s) { return engine.analyzeMessage(s).assessment().classification(); }

    @Test void ordinarySitesLookSafe() {
        assertEquals(Classification.SAFE_LOOKING, url("https://www.wikipedia.org"));
        assertEquals(Classification.SAFE_LOOKING, url("accounts.google.com/signin"));
    }

    @Test void brandOnForeignDomainIsHighRisk() {
        assertEquals(Classification.HIGH_RISK, url("https://paypal-security-login.xyz/verify"));
    }

    @Test void homographIsDetectedFromPunycode() {
        var r = engine.analyzeUrl("https://xn--pple-43d.com/");
        assertEquals(Classification.HIGH_RISK, r.assessment().classification());
        assertTrue(r.assessment().findings().stream().anyMatch(f -> f.finding().ruleId().equals("mixed_script_host")));
    }

    @Test void userinfoTrickAndIpHostsAreFlagged() {
        assertNotEquals(Classification.SAFE_LOOKING, url("https://paypal.com@evil.example/signin"));
        assertNotEquals(Classification.SAFE_LOOKING, url("http://192.168.0.1/login"));
    }

    @Test void shortenerIsSuspiciousNotHighRisk() { assertEquals(Classification.SUSPICIOUS, url("http://bit.ly/3Xk9a7")); }

    @Test void httpsDoesNotProveSafety() {
        var r = engine.analyzeUrl("https://paypal-security-login.xyz/verify");
        assertTrue(r.url().scheme().equals("https") && r.assessment().score() >= 60);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "javascript:alert(1)", "ftp://files.example.com", "not a url", "https://exa mple.com", "https://example.com:99999/"})
    void invalidUrlsAreRejected(String s) { assertThrows(InvalidInputException.class, () -> engine.analyzeUrl(s)); }

    @Test void urlCredentialsAreNeverRetained() {
        var u = engine.analyzeUrl("https://user:secret@example.com/").url();
        assertFalse(u.normalized().contains("secret"));
        assertEquals("user", u.userName());
    }

    @Test void phishingMessageIsHighRisk() {
        assertEquals(Classification.HIGH_RISK, msg("URGENT: Your account has been suspended. Verify your password immediately at http://secure-paypa1-login.top/verify or it will be closed within 24 hours."));
    }

    @Test void legitimateUrgentMessagesStaySafeLooking() {
        assertEquals(Classification.SAFE_LOOKING, msg("Your verification code is 482913. It expires in 10 minutes. Do not share this code with anyone."));
        assertEquals(Classification.SAFE_LOOKING, msg("Reminder: your electricity bill is due tomorrow. Please pay via the official app to avoid late fees."));
        assertEquals(Classification.SAFE_LOOKING, msg("Hi, your appointment is confirmed for Tuesday at 10:30. Reply STOP to opt out."));
    }

    @Test void otpRequestIsHighRisk() { assertEquals(Classification.HIGH_RISK, msg("This is your bank. Please share your OTP to avoid account blocked. Don't tell anyone.")); }

    @Test void messageFindingsCarryHighlightSpans() {
        var f = engine.analyzeMessage("Please verify your password now").assessment().findings();
        assertTrue(f.stream().anyMatch(s -> s.finding().ruleId().equals("credential_request") && !s.finding().spans().isEmpty()));
    }

    @Test void oversizedMessageRejected() { assertThrows(InvalidInputException.class, () -> engine.analyzeMessage("x".repeat(10_001))); }
}
