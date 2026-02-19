package pe.edu.vallegrande.sigei.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

     @Bean
     @Profile("dev")
     public SecurityWebFilterChain securityWebFilterChainDev(ServerHttpSecurity http) {
          return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(exchanges -> exchanges
                              .anyExchange().permitAll())
                    .build();
     }

     @Bean
     @Profile("prod")
     public SecurityWebFilterChain securityWebFilterChainProd(ServerHttpSecurity http) {
          return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(exchanges -> exchanges
                              .pathMatchers("/actuator/**").permitAll()
                              .pathMatchers("/fallback/**").permitAll()
                              .anyExchange().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2
                              .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())))
                    .build();
     }

     private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
          JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
          jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
          return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
     }

     static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
          @Override
          public Collection<GrantedAuthority> convert(Jwt jwt) {
               Map<String, Object> realmAccess = jwt.getClaim("realm_access");
               if (realmAccess == null || realmAccess.get("roles") == null) {
                    return List.of();
               }

               @SuppressWarnings("unchecked")
               List<String> roles = (List<String>) realmAccess.get("roles");
               return roles.stream()
                         .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                         .collect(Collectors.toList());
          }
     }
}
