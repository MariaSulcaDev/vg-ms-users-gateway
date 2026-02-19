package pe.edu.vallegrande.sigei.vgmsusermanagement.application.usecases;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions.DuplicateDocumentNumberException;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.in.ICreateUserUseCase;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IUserEventPublisher;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IUserRepository;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateUserUseCaseImpl implements ICreateUserUseCase {

    private final IUserRepository userRepository;
    private final IUserEventPublisher eventPublisher;
    private final IKeycloakClient keycloakClient;

    @Override
    public Mono<User> execute(User user) {
        return userRepository.existsByDocumentNumber(user.getDocumentNumber())
            .flatMap(exists -> {
                if (Boolean.TRUE.equals(exists)) {
                    return Mono.error(new DuplicateDocumentNumberException(user.getDocumentNumber()));
                }
                return userRepository.save(user);
            })
            .flatMap(saved -> keycloakClient.createUser(
                    saved.getUserName(),
                    saved.getDocumentNumber(),
                    saved.getEmail(),
                    saved.getFirstName(),
                    saved.getLastName(),
                    saved.getRole().name())
                .doOnError(e -> log.error("Error registrando en Keycloak: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .thenReturn(saved))
            .flatMap(saved -> eventPublisher.publishUserCreated(saved)
                .thenReturn(saved));
    }
}
