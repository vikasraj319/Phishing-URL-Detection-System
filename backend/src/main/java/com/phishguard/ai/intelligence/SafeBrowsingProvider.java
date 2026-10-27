package com.phishguard.ai.intelligence;

import com.phishguard.ai.config.AppProperties;
import com.phishguard.ai.dto.ApiDtos.ProviderResultDto;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Google Safe Browsing v4 threatMatches:find. Check the current terms and quotas before production use. */
@Component
public class SafeBrowsingProvider implements ThreatIntelProvider {
    private static final String ENDPOINT = "https://safebrowsing.googleapis.com/v4/threatMatches:find";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};
    private final String key;
    private final RestClient client;

    public SafeBrowsingProvider(AppProperties props) {
        this.key = props.intel().safeBrowsingApiKey();
        this.client = ProviderHttp.client(props.intel().timeoutMs());
    }

    @Override public String name() { return "Google Safe Browsing"; }
    @Override public boolean configured() { return key != null && !key.isBlank(); }

    @Override
    public ProviderResultDto check(String url) throws ProviderException {
        Map<String, Object> body = Map.of(
            "client", Map.of("clientId", "phishguard-ai", "clientVersion", "1.0.0"),
            "threatInfo", Map.of(
                "threatTypes", List.of("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"),
                "platformTypes", List.of("ANY_PLATFORM"),
                "threatEntryTypes", List.of("URL"),
                "threatEntries", List.of(Map.of("url", url))));
        Map<String, Object> resp;
        try {
            resp = client.post().uri(ENDPOINT).header("x-goog-api-key", key)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(MAP);
        } catch (RestClientResponseException e) {
            throw new ProviderException(e.getStatusCode().value() == 429 ? "Provider quota exceeded." : "Provider returned HTTP " + e.getStatusCode().value() + ".");
        } catch (RuntimeException e) {
            throw new ProviderException("Provider could not be reached (timeout or network error).");
        }
        return ProviderResponses.safeBrowsing(name(), resp);
    }
}
