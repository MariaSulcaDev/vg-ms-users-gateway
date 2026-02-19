package pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.adapters.out.external;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
public class InstitutionClientImpl {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    public InstitutionClientImpl(
        WebClient.Builder webClientBuilder,
        @Value("${services.institution.url}") String institutionUrl) {
        this.webClient = webClientBuilder.baseUrl(institutionUrl).build();
        this.circuitBreaker = CircuitBreaker.of("institutionService",
            CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .slowCallRateThreshold(80)
                .build());
    }

    public Mono<Boolean> existsById(String institutionId) {
        return webClient.get()
            .uri("/api/institutions/{id}", institutionId)
            .retrieve()
            .bodyToMono(Object.class)
            .map(response -> true)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(e -> {
                log.warn("Institución no encontrada o servicio no disponible: {}", e.getMessage());
                return Mono.just(false);
            });
    }
}