package pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.adapters.out.external;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IKeycloakClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KeycloakClientImpl implements IKeycloakClient {

    private final WebClient webClient;
    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final CircuitBreaker circuitBreaker;

    public KeycloakClientImpl(
        WebClient.Builder webClientBuilder,
        @Value("${keycloak.admin.server-url}") String serverUrl,
        @Value("${keycloak.admin.realm}") String realm,
        @Value("${keycloak.admin.client-id}") String clientId,
        @Value("${keycloak.admin.client-secret}") String clientSecret) {
        this.webClient = webClientBuilder.build();
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.circuitBreaker = CircuitBreaker.of("keycloakService",
            CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .slowCallRateThreshold(80)
                .build());
    }

    @Override
    public Mono<Void> createUser(String username, String password, String email,
                                 String firstName, String lastName, String role) {
        return getAdminToken()
            .flatMap(token -> createKeycloakUser(token, username, password, email, firstName, lastName))
            .flatMap(token -> getKeycloakUserId(token, username))
            .flatMap(result -> assignRealmRole(result.get("token"), result.get("userId"), role))
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .doOnSuccess(v -> log.info("Usuario creado en Keycloak: {}", username))
            .doOnError(e -> log.error("Error creando usuario en Keycloak: {}", e.getMessage()));
    }

    private Mono<String> getAdminToken() {
        return webClient.post()
            .uri(serverUrl + "/realms/" + realm + "/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters
                .fromFormData("grant_type", "client_credentials")
                .with("client_id", clientId)
                .with("client_secret", clientSecret))
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> (String) response.get("access_token"));
    }

    @SuppressWarnings("unchecked")
    private Mono<String> createKeycloakUser(String token, String username, String password,
                                            String email, String firstName, String lastName) {
        Map<String, Object> userRepresentation = Map.of(
            "username", username,
            "email", email != null ? email : "",
            "firstName", firstName,
            "lastName", lastName,
            "enabled", true,
            "credentials", List.of(Map.of(
                "type", "password",
                "value", password,
                "temporary", false)));

        return webClient.post()
            .uri(serverUrl + "/admin/realms/" + realm + "/users")
            .headers(h -> h.setBearerAuth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(userRepresentation)
            .retrieve()
            .toBodilessEntity()
            .thenReturn(token);
    }

    @SuppressWarnings("unchecked")
    private Mono<Map<String, String>> getKeycloakUserId(String token, String username) {
        return webClient.get()
            .uri(serverUrl + "/admin/realms/" + realm + "/users?username=" + username + "&exact=true")
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .bodyToMono(List.class)
            .map(users -> {
                Map<String, Object> user = (Map<String, Object>) users.get(0);
                return Map.of("token", token, "userId", (String) user.get("id"));
            });
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> assignRealmRole(String token, String userId, String roleName) {
        return webClient.get()
            .uri(serverUrl + "/admin/realms/" + realm + "/roles/" + roleName.toLowerCase())
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .bodyToMono(Map.class)
            .flatMap(role -> webClient.post()
                .uri(serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(role))
                .retrieve()
                .toBodilessEntity()
                .then());
    }
}
