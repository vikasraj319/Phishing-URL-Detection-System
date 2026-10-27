package com.phishguard.ai.intelligence;

/** Carries only text that is safe to show to users (never URLs, keys or upstream messages). */
public class ProviderException extends Exception {
    private static final long serialVersionUID = 1L;
    public ProviderException(String publicDetail) { super(publicDetail); }
}
