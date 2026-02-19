package pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.persistence.repositories;

import org.springframework.data.r2dbc.repository.Query;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.persistence.entities.UserEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserR2dbcRepository extends ReactiveCrudRepository<UserEntity, String> {

    Flux<UserEntity> findByStatus(String status);

    Flux<UserEntity> findByRoleAndStatus(String role, String status);

    Flux<UserEntity> findByInstitutionId(String institutionId);

    Mono<Boolean> existsByDocumentNumber(String documentNumber);

    @Query("SELECT COUNT(*) > 0 FROM users WHERE document_number = :documentNumber AND id != :id")
    Mono<Boolean> existsByDocumentNumberAndIdNot(String documentNumber, String id);
}