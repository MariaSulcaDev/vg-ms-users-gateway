package pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out;

import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserRole;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IUserRepository {
    Mono<User> save(User user);

    Mono<User> findById(String id);

    Flux<User> findAll();

    Flux<User> findByStatus(UserStatus status);

    Flux<User> findByRoleAndStatus(UserRole role, UserStatus status);

    Flux<User> findByInstitutionId(String institutionId);

    Mono<Boolean> existsByDocumentNumber(String documentNumber);

    Mono<Boolean> existsByDocumentNumberAndIdNot(String documentNumber, String id);
}
