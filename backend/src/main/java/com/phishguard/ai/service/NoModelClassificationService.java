package com.phishguard.ai.service;

import com.phishguard.ai.detection.UrlFeatures;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NoModelClassificationService implements AiClassificationService {
    @Override public Optional<Prediction> classify(UrlFeatures features) { return Optional.empty(); }
}
