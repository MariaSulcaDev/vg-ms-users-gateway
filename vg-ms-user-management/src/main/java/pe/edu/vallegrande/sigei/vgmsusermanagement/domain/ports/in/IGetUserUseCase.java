package pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.in;

import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserRole;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IGetUserUseCase {
    Flux<User> findAll();

    Mono<User> findById(String id);

    Flux<User> findByStatus(UserStatus status);

    Flux<User> findByRoleAndStatus(UserRole role, UserStatus status);

    Flux<User> findByInstitutionId(String institutionId);
}
