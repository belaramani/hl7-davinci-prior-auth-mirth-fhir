package medibridgepayermock.medibridgepayermock;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@SuppressWarnings({"rawtypes", "unchecked"})
public class SmartAuthController {

    // Registered Provider Client Credentials (Mirth Connect Gateway)
    public static final String EXPECTED_CLIENT_ID = "mirth-ehr-gateway";
    public static final String EXPECTED_CLIENT_SECRET = "davinci-smart-secret-2026";

    // Active token store: token -> expiry epoch seconds
    private static final Map<String, Long> ACTIVE_TOKENS = new ConcurrentHashMap<>();

    // 1. SMART on FHIR Well-Known Discovery Endpoint
    @GetMapping(value = "/.well-known/smart-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getSmartConfiguration() {
        var config = new LinkedHashMap<String, Object>();
        config.put("issuer", "http://localhost:8085");
        config.put("token_endpoint", "http://localhost:8085/oauth2/token");
        config.put("grant_types_supported", List.of("client_credentials"));
        config.put("token_endpoint_auth_methods_supported", List.of("client_secret_post", "client_secret_basic"));
        config.put("scopes_supported", List.of(
                "system/Questionnaire.read",
                "system/Claim.write",
                "system/ClaimResponse.read"
        ));
        config.put("capabilities", List.of(
                "client-confidential-symmetric",
                "context-passthrough-banner"
        ));
        return config;
    }

    // 2. OAuth 2.0 Backend Services Token Endpoint
    @PostMapping(
            value = "/oauth2/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity issueAccessToken(
            @RequestParam(value = "grant_type", required = false) String grantType,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "scope", defaultValue = "system/Questionnaire.read system/Claim.write") String scope
    ) {
        if (!"client_credentials".equals(grantType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "unsupported_grant_type", "error_description", "Use grant_type=client_credentials"));
        }

        if (!EXPECTED_CLIENT_ID.equals(clientId) || !EXPECTED_CLIENT_SECRET.equals(clientSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_client", "error_description", "Invalid SMART client_id or client_secret"));
        }

        String accessToken = "smart_at_" + UUID.randomUUID().toString().replace("-", "");
        long expiresInSeconds = 3600L;
        ACTIVE_TOKENS.put(accessToken, Instant.now().getEpochSecond() + expiresInSeconds);

        var tokenResponse = new LinkedHashMap<String, Object>();
        tokenResponse.put("access_token", accessToken);
        tokenResponse.put("token_type", "Bearer");
        tokenResponse.put("expires_in", expiresInSeconds);
        tokenResponse.put("scope", scope);

        return ResponseEntity.ok(tokenResponse);
    }

    // Helper called by SmartSecurityInterceptor to validate Bearer tokens
    public static boolean isValidToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Long expiry = ACTIVE_TOKENS.get(token);
        if (expiry == null) {
            return false;
        }
        if (Instant.now().getEpochSecond() > expiry) {
            ACTIVE_TOKENS.remove(token);
            return false;
        }
        return true;
    }
}