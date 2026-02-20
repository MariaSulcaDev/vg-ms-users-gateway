package pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out;

import reactor.core.publisher.Mono;

public interface IKeycloakClient {
    Mono<Void> createUser(String username, String password, String email,
            String firstName, String lastName, String role,
            String institutionId);

}
