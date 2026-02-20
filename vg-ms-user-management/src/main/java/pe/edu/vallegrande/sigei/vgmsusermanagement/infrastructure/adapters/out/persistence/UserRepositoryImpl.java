package pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.adapters.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserRole;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserStatus;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IUserRepository;
import pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.persistence.mappers.UserPersistenceMapper;
import pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.persistence.repositories.UserR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements IUserRepository {

    private final UserR2dbcRepository r2dbcRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public Mono<User> save(User user) {
        return r2dbcRepository.save(mapper.toEntity(user))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<User> findById(String id) {
        return r2dbcRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<User> findAll() {
        return r2dbcRepository.findAll()
                .map(mapper::toDomain);
    }

    @Override
    public Flux<User> findByStatus(UserStatus status) {
        return r2dbcRepository.findByStatus(status.name())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<User> findByRoleAndStatus(UserRole role, UserStatus status) {
        return r2dbcRepository.findByRoleAndStatus(role.name(), status.name())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<User> findByInstitutionId(String institutionId) {
        return r2dbcRepository.findByInstitutionId(institutionId)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByDocumentNumber(String documentNumber) {
        return r2dbcRepository.existsByDocumentNumber(documentNumber);
    }

    @Override
    public Mono<Boolean> existsByDocumentNumberAndIdNot(String documentNumber, String id) {
        return r2dbcRepository.existsByDocumentNumberAndIdNot(documentNumber, id);
    }

    @Override
    public Mono<Boolean> existsByUserName(String userName) {
        return r2dbcRepository.existsByUserName(userName);
    }
}
