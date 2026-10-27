package com.phishguard.ai.detection;

/** Character offsets [start, end) into the analysed message, used for highlighting. */
public record Span(int start, int end) {}
