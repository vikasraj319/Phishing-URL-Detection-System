package com.phishguard.ai.service;

import com.phishguard.ai.detection.Classification;
import com.phishguard.ai.detection.UrlFeatures;
import java.util.Optional;

/**
 * Extension point for an optional trained model. Model confidence is deliberately separate from the heuristic
 * risk score and is not blended into it. No model ships with this build, and the API does not expose this yet.
 */
public interface AiClassificationService {
    record Prediction(String model, double confidence, Classification label) {}
    Optional<Prediction> classify(UrlFeatures features);
}
