package pe.edu.vallegrande.sigei.vgmsusermanagement.application.usecases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions.UserNotFoundException;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserStatus;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.in.IGetUserUseCase;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IUserRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetUserUseCaseImpl implements IGetUserUseCase {

    private final IUserRepository userRepository;

    @Override
    public Flux<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Mono<User> findById(String id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new UserNotFoundException(id)));
    }

    @Override
    public Flux<User> findByStatus(UserStatus status) {
        return userRepository.findByStatus(status);
    }

    @Override
    public Flux<User> findByRoleAndStatus(UserRole role, UserStatus status) {
        return userRepository.findByRoleAndStatus(role, status);
    }

    @Override
    public Flux<User> findByInstitutionId(String institutionId) {
        return userRepository.findByInstitutionId(institutionId);
    }
}
