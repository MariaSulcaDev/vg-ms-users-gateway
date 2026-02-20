package pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KeycloakMapperInitializer {

     private final WebClient webClient;
     private final String serverUrl;
     private final String realm;
     private final String clientId;
     private final String clientSecret;

     public KeycloakMapperInitializer(
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
     }

     @EventListener(ApplicationReadyEvent.class)
     public void initializeMappers() {
          log.info("Verificando Protocol Mappers en Keycloak...");
          getAdminToken()
                    .flatMap(this::getClientInternalId)
                    .flatMap(result -> ensureInstitutionIdMapper(result.get("token"), result.get("clientUuid")))
                    .subscribe(
                              v -> log.info("Protocol Mappers de Keycloak verificados correctamente"),
                              e -> log.warn(
                                        "No se pudieron verificar los mappers de Keycloak (puede que no esté disponible): {}",
                                        e.getMessage()));
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
     private Mono<Map<String, String>> getClientInternalId(String token) {
          return webClient.get()
                    .uri(serverUrl + "/admin/realms/" + realm + "/clients?clientId=" + clientId)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(List.class)
                    .map(clients -> {
                         if (clients.isEmpty()) {
                              throw new RuntimeException("Cliente '" + clientId + "' no encontrado en Keycloak");
                         }
                         Map<String, Object> client = (Map<String, Object>) clients.get(0);
                         String uuid = (String) client.get("id");
                         log.info("Cliente Keycloak '{}' encontrado con UUID: {}", clientId, uuid);
                         return Map.of("token", token, "clientUuid", uuid);
                    });
     }

     @SuppressWarnings("unchecked")
     private Mono<Void> ensureInstitutionIdMapper(String token, String clientUuid) {
          return webClient.get()
                    .uri(serverUrl + "/admin/realms/" + realm + "/clients/" + clientUuid + "/protocol-mappers/models")
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(List.class)
                    .flatMap(mappers -> {
                         boolean exists = ((List<Map<String, Object>>) mappers).stream()
                                   .anyMatch(m -> "institutionId".equals(m.get("name")));

                         if (exists) {
                              log.info("Protocol Mapper 'institutionId' ya existe, no se requiere creación");
                              return Mono.empty();
                         }

                         log.info("Creando Protocol Mapper 'institutionId' en cliente '{}'...", clientId);
                         return createInstitutionIdMapper(token, clientUuid);
                    });
     }

     private Mono<Void> createInstitutionIdMapper(String token, String clientUuid) {
          Map<String, Object> mapper = Map.ofEntries(
                    Map.entry("name", "institutionId"),
                    Map.entry("protocol", "openid-connect"),
                    Map.entry("protocolMapper", "oidc-usermodel-attribute-mapper"),
                    Map.entry("consentRequired", false),
                    Map.entry("config", Map.of(
                              "user.attribute", "institutionId",
                              "claim.name", "institutionId",
                              "jsonType.label", "String",
                              "id.token.claim", "true",
                              "access.token.claim", "true",
                              "userinfo.token.claim", "true",
                              "multivalued", "false",
                              "aggregate.attrs", "false")));

          return webClient.post()
                    .uri(serverUrl + "/admin/realms/" + realm + "/clients/" + clientUuid + "/protocol-mappers/models")
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(mapper)
                    .retrieve()
                    .toBodilessEntity()
                    .doOnSuccess(r -> log.info("Protocol Mapper 'institutionId' creado exitosamente"))
                    .then();
     }
}
