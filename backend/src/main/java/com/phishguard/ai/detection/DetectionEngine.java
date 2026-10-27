package com.phishguard.ai.detection;

import java.util.*;
import java.util.regex.*;

/**
 * Framework-free facade for the local detection pipeline (normalise -> extract -> rules -> score).
 * No network access happens here: SAFE_LOOKING means "no sufficient indicators found", never "verified safe".
 * Threat-intelligence and ML results are layered on by the Spring services.
 */
public final class DetectionEngine {
    public static final int MAX_MESSAGE_LENGTH = 10_000;
    private static final Pattern URL_IN_TEXT = Pattern.compile(
        "(?i)\\bhttps?://[^\\s<>\"')]+|\\bwww\\.[^\\s<>\"')]+|\\b(?:bit\\.ly|tinyurl\\.com|t\\.co|goo\\.gl|ow\\.ly|is\\.gd|cutt\\.ly|rb\\.gy|tiny\\.cc)/[^\\s<>\"')]+");

    private final RuleBasedUrlDetector urlRules;
    private final MessageRuleBasedDetector messageRules;
    private final RiskScorer scorer;

    public DetectionEngine() { this(new RuleWeights(), Thresholds.defaults()); }

    public DetectionEngine(RuleWeights weights, Thresholds thresholds) {
        this.urlRules = new RuleBasedUrlDetector(weights);
        this.messageRules = new MessageRuleBasedDetector(weights);
        this.scorer = new RiskScorer(thresholds);
    }

    public UrlAnalysisResult analyzeUrl(String input) {
        NormalizedUrl u = UrlNormalizer.normalize(input);
        UrlFeatures f = UrlFeatureExtractor.extract(u);
        return new UrlAnalysisResult(u, f, scorer.assess(urlRules.evaluate(u, f)));
    }

    public MessageAnalysisResult analyzeMessage(String text) {
        if (text == null || text.isBlank()) throw new InvalidInputException("Enter a message to analyze.");
        if (text.length() > MAX_MESSAGE_LENGTH) throw new InvalidInputException("Message is longer than " + MAX_MESSAGE_LENGTH + " characters.");

        List<Finding> findings = new ArrayList<>(messageRules.evaluate(text));
        List<String> urls = extractUrls(text);
        List<UrlAnalysisResult> results = new ArrayList<>();
        for (String url : urls) {
            try { results.add(analyzeUrl(url)); } catch (InvalidInputException ignored) { /* not a parseable link */ }
        }
        results.stream().max(Comparator.comparingInt(r -> r.assessment().score())).ifPresent(worst -> {
            int s = worst.assessment().score();
            if (s >= 30) findings.add(Finding.of("suspicious_link", "Message contains a risky link", Severity.HIGH,
                "A link in the message scored " + s + "/100 on its own. See the URL findings for the reasons.",
                worst.url().normalized(), Math.min(60, (int) Math.round(s * 0.7))));
        });
        return new MessageAnalysisResult(scorer.assess(findings), urls, List.copyOf(results), text.length());
    }

    static List<String> extractUrls(String text) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        Matcher m = URL_IN_TEXT.matcher(text);
        while (m.find() && out.size() < 10) out.add(m.group().replaceAll("[.,;:!?]+$", ""));
        return List.copyOf(out);
    }
}
