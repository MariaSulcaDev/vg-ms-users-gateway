package pe.edu.vallegrande.vgmsusers.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] SWAGGER_PATHS = {
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/webjars/**"
    };

    private static final String[] ACTUATOR_PATHS = {
        "/actuator/health",
        "/actuator/info"
    };

    @Bean
    @Profile("dev")
    public SecurityWebFilterChain devSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(SWAGGER_PATHS).permitAll()
                .pathMatchers(ACTUATOR_PATHS).permitAll()
                .anyExchange().permitAll()
            )
            .build();
    }

    @Bean
    @Profile("prod")
    public SecurityWebFilterChain prodSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(SWAGGER_PATHS).denyAll()
                .pathMatchers(ACTUATOR_PATHS).permitAll()
                .anyExchange().permitAll()
            )
            .build();
    }
}
