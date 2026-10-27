package com.phishguard.ai.intelligence;

import com.phishguard.ai.config.AppProperties;
import com.phishguard.ai.dto.ApiDtos.ProviderResultDto;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** VirusTotal v3 URL report lookup (read only; it never submits the URL for scanning). Respect the API quota terms. */
@Component
public class VirusTotalProvider implements ThreatIntelProvider {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};
    private final String key;
    private final RestClient client;

    public VirusTotalProvider(AppProperties props) {
        this.key = props.intel().virusTotalApiKey();
        this.client = ProviderHttp.client(props.intel().timeoutMs());
    }

    @Override public String name() { return "VirusTotal"; }
    @Override public boolean configured() { return key != null && !key.isBlank(); }

    private record Reply(int status, Map<String, Object> body) {}

    @Override
    public ProviderResultDto check(String url) throws ProviderException {
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(url.getBytes(StandardCharsets.UTF_8));
        Reply reply;
        try {
            reply = client.get().uri("https://www.virustotal.com/api/v3/urls/{id}", id).header("x-apikey", key)
                .exchange((req, res) -> new Reply(res.getStatusCode().value(), res.getStatusCode().value() == 200 ? res.bodyTo(MAP) : null));
        } catch (RuntimeException e) {
            throw new ProviderException("Provider could not be reached (timeout or network error).");
        }
        return ProviderResponses.virusTotal(name(), reply.status(), reply.body());
    }
}
