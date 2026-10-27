package com.phishguard.ai.detection;

import java.util.*;
import java.util.regex.*;

/** Deterministic social-engineering patterns. Reports matched spans so the UI can highlight them. */
public final class MessageRuleBasedDetector {
    private record Rule(String id, String title, Severity sev, String why, Pattern pattern) {}

    private static Pattern p(String re) { return Pattern.compile(re, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE); }
    private static final String SENSITIVE = "(password|credentials?|log ?in details|sign[- ]?in details|passcode|security (?:details|questions?)"
        + "|card (?:number|details)|cvv|ssn|social security|bank (?:details|account)|account (?:details|number|information))";

    private static final List<Rule> RULES = List.of(
        new Rule("urgency", "Urgency or time pressure", Severity.LOW,
            "Pressure to act quickly is used to stop people from checking. On its own it is common in legitimate reminders.",
            p("\\b(urgent(?:ly)?|immediately|right away|act now|final (?:notice|warning)|last chance|asap"
              + "|within \\d+ (?:hours?|minutes?|days?)|expires? (?:today|soon|in \\d+ \\w+))\\b")),
        new Rule("account_threat", "Threat to your account", Severity.MEDIUM,
            "Warning that an account will be locked or closed is a classic way to provoke a rushed reaction.",
            p("\\b(?:account|card|access|profile)\\b.{0,40}\\b(?:suspend(?:ed)?|locked|blocked|deactivated|terminated|restricted|closed|compromised)\\b"
              + "|\\b(?:will be|has been|have been|is being)\\s+(?:suspended|locked|blocked|deactivated|terminated|restricted|closed)\\b")),
        new Rule("credential_request", "Request for credentials or financial details", Severity.HIGH,
            "Genuine organisations do not ask you to send or confirm passwords or card details through messages or links.",
            p("\\b(?:verify|confirm|update|validate|re-?enter|provide|enter|submit|send)\\b.{0,30}\\b" + SENSITIVE + "\\b")),
        new Rule("otp_request", "Request for a one-time code", Severity.HIGH,
            "One-time codes exist to prove it is you. Anyone asking you to hand one over is trying to take over an account.",
            p("(?<!not )(?<!never )(?<!n't )\\b(?:send|share|reply with|provide|tell|read out|forward|confirm|enter)\\b.{0,40}"
              + "\\b(?:otp|one[- ]time (?:password|code|pin)|verification code|security code|pin|passcode)\\b")),
        new Rule("payment_request", "Unusual payment request", Severity.MEDIUM,
            "Demands for gift cards, crypto, wire transfers or specific payments under pressure are typical of scams.",
            p("\\b(?:buy|send|pay|purchase|transfer)\\b.{0,40}\\b(?:gift cards?|bitcoin|crypto(?:currency)?|western union|wire transfer)\\b"
              + "|\\b(?:pay|send|transfer|deposit)\\b.{0,25}(?:[$₹€£]\\s?\\d|\\b\\d[\\d,.]*\\s?(?:usd|inr|dollars|rupees)\\b)")),
        new Rule("prize_scam", "Prize or reward claim", Severity.HIGH,
            "Unexpected winnings that must be claimed through a link or payment are a standard advance-fee or data-harvesting lure.",
            p("\\b(?:won|winner|selected|lucky|congratulations)\\b.{0,40}(?:prize|lottery|[$₹€£]\\s?\\d|gift card|reward|cash|iphone)"
              + "|\\bclaim (?:your|the) (?:prize|reward|gift|winnings?)\\b")),
        new Rule("impersonation_claim", "Claims to be an authority or known brand", Severity.LOW,
            "Scammers pose as banks, tax offices, couriers or support teams. Real ones also write to you, so this only matters alongside other signals.",
            p("\\b(?:this is|we are|from|official)\\b.{0,25}\\b(?:your bank|the bank|tax (?:department|office)|irs|customer (?:care|support)"
              + "|security team|support team|police|courier|amazon|paypal|microsoft|apple)\\b")),
        new Rule("bypass_verification", "Asks you to bypass normal checks or stay quiet", Severity.HIGH,
            "Telling you to keep something secret, avoid calling your bank, or install remote-access tools is a strong scam signature.",
            p("\\b(?:do not|don't|never)\\s+(?:tell|inform|contact|call)\\b.{0,30}\\b(?:bank|anyone|family|support|police)\\b"
              + "|\\bkeep this (?:secret|confidential)\\b|\\b(?:skip|bypass)\\b.{0,15}\\bverification\\b|\\b(?:anydesk|teamviewer)\\b")),
        new Rule("link_click_prompt", "Prompt to click a link", Severity.INFO,
            "Prompts to tap a link are normal, but they are how phishing messages move you to a fake page.",
            p("\\b(?:click|tap|follow)\\b.{0,25}\\b(?:link|here|below)\\b"))
    );
    private static final Set<String> SENSITIVE_IDS = Set.of("credential_request", "otp_request", "payment_request", "account_threat");

    private final RuleWeights weights;

    public MessageRuleBasedDetector(RuleWeights weights) { this.weights = weights; }

    public List<Finding> evaluate(String text) {
        List<Finding> out = new ArrayList<>();
        Set<String> fired = new HashSet<>();
        for (Rule rule : RULES) {
            Matcher m = rule.pattern().matcher(text);
            List<Span> spans = new ArrayList<>();
            String first = null;
            while (m.find() && spans.size() < 10) {
                spans.add(new Span(m.start(), m.end()));
                if (first == null) first = m.group().strip();
            }
            if (spans.isEmpty()) continue;
            fired.add(rule.id());
            out.add(new Finding(rule.id(), rule.title(), rule.sev(), rule.why(), "\"" + clip(first) + "\"",
                weights.weight(rule.id()), spans));
        }
        if (fired.contains("urgency") && fired.stream().anyMatch(SENSITIVE_IDS::contains)) {
            out.add(Finding.of("urgency_with_sensitive_request", "Urgency combined with a sensitive demand", Severity.MEDIUM,
                "Time pressure together with a request for credentials, codes, payment or an account threat is much stronger than either alone.",
                "Urgency plus: " + String.join(", ", fired.stream().filter(SENSITIVE_IDS::contains).sorted().toList()),
                weights.weight("urgency_with_sensitive_request")));
        }
        return out;
    }

    private static String clip(String s) { return s.length() > 120 ? s.substring(0, 117) + "..." : s; }
}
