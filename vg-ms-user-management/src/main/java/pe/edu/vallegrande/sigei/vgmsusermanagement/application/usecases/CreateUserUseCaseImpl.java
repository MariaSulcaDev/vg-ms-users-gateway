package pe.edu.vallegrande.sigei.vgmsusermanagement.application.usecases;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions.DuplicateDocumentNumberException;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.in.ICreateUserUseCase;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IKeycloakClient;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IUserEventPublisher;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IUserRepository;
import reactor.core.publisher.Mono;

import java.text.Normalizer;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateUserUseCaseImpl implements ICreateUserUseCase {

    private static final String DOMAIN = "@sigei.gob.pe";

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
                    return generateUniqueUserName(user)
                            .flatMap(userName -> {
                                user.setUserName(userName);
                                return userRepository.save(user);
                            });
                })
                .flatMap(saved -> keycloakClient.createUser(
                        saved.getUserName(),
                        saved.getDocumentNumber(),
                        saved.getEmail(),
                        saved.getFirstName(),
                        saved.getLastName(),
                        saved.getRole().name(),
                        saved.getInstitutionId())
                        .doOnError(e -> log.error("Error registrando en Keycloak: {}", e.getMessage()))
                        .onErrorResume(e -> Mono.empty())
                        .thenReturn(saved))
                .flatMap(saved -> eventPublisher.publishUserCreated(saved)
                        .thenReturn(saved));
    }

    /**
     * Genera un userName único con formato:
     * 1) primerNombre.primerApellido@sigei.gob.pe
     * 2) Si ya existe → primerNombre.primerApellido.x@sigei.gob.pe
     * donde x = primera letra del apellido materno
     */
    private Mono<String> generateUniqueUserName(User user) {
        String firstName = normalize(user.getFirstName().trim().split("\\s+")[0]);
        String lastName = normalize(user.getLastName().trim().split("\\s+")[0]);

        String baseUserName = firstName + "." + lastName + DOMAIN;

        return userRepository.existsByUserName(baseUserName)
                .flatMap(exists -> {
                    if (Boolean.FALSE.equals(exists)) {
                        log.info("Username generado: {}", baseUserName);
                        return Mono.just(baseUserName);
                    }

                    // Si ya existe, usar primera letra del apellido materno
                    String motherLastName = user.getMotherLastName();
                    if (motherLastName != null && !motherLastName.isBlank()) {
                        String initial = normalize(motherLastName.trim().substring(0, 1));
                        String altUserName = firstName + "." + lastName + "." + initial + DOMAIN;
                        log.info("Username con inicial materna: {}", altUserName);
                        return Mono.just(altUserName);
                    }

                    // Sin apellido materno, fallback con número de documento
                    String fallbackUserName = firstName + "." + lastName + "."
                            + user.getDocumentNumber().substring(user.getDocumentNumber().length() - 3) + DOMAIN;
                    log.info("Username fallback: {}", fallbackUserName);
                    return Mono.just(fallbackUserName);
                });
    }

    /**
     * Normaliza texto: quita tildes/acentos y convierte a minúsculas.
     * Ej: "María" → "maria", "López" → "lopez"
     */
    private String normalize(String text) {
        String normalized = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }
}
