package pe.edu.vallegrande.sigei.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtClaimsForwardFilter implements GlobalFilter, Ordered {

     @Override
     public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
          return ReactiveSecurityContextHolder.getContext()
                    .filter(ctx -> ctx.getAuthentication() instanceof JwtAuthenticationToken)
                    .map(ctx -> (JwtAuthenticationToken) ctx.getAuthentication())
                    .flatMap(auth -> {
                         Jwt jwt = auth.getToken();

                         ServerWebExchange mutated = exchange.mutate()
                                   .request(r -> r
                                             .header("X-User-Id", jwt.getSubject())
                                             .header("X-Username", jwt.getClaimAsString("preferred_username"))
                                             .header("X-Institution-Id", extractInstitutionId(jwt))
                                             .header("X-User-Role", extractRole(jwt)))
                                   .build();

                         log.debug("JWT claims forwarded: sub={}, institution={}, role={}",
                                   jwt.getSubject(),
                                   extractInstitutionId(jwt),
                                   extractRole(jwt));

                         return chain.filter(mutated);
                    })
                    .switchIfEmpty(chain.filter(exchange));
     }

     @SuppressWarnings("unchecked")
     private String extractInstitutionId(Jwt jwt) {
          Object institutionId = jwt.getClaim("institutionId");
          if (institutionId instanceof String) {
               return (String) institutionId;
          }
          if (institutionId instanceof List) {
               List<String> list = (List<String>) institutionId;
               return list.isEmpty() ? "" : list.get(0);
          }
          return "";
     }

     @SuppressWarnings("unchecked")
     private String extractRole(Jwt jwt) {
          Map<String, Object> realmAccess = jwt.getClaim("realm_access");
          if (realmAccess == null || realmAccess.get("roles") == null) {
               return "";
          }
          List<String> roles = (List<String>) realmAccess.get("roles");
          return roles.stream()
                    .filter(r -> !r.startsWith("default-roles-")
                              && !r.equals("offline_access")
                              && !r.equals("uma_authorization"))
                    .findFirst()
                    .orElse("");
     }

     @Override
     public int getOrder() {
          return -1;
     }
}
