package pe.edu.vallegrande.sigei.vgmsusermanagement.application.usecases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions.UserNotFoundException;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserStatus;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.in.IDeleteUserUseCase;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IUserEventPublisher;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IUserRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeleteUserUseCaseImpl implements IDeleteUserUseCase {

    private final IUserRepository userRepository;
    private final IUserEventPublisher eventPublisher;

    @Override
    public Mono<User> execute(String id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
            .flatMap(user -> {
                user.setStatus(UserStatus.INACTIVE);
                user.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(user);
            })
            .flatMap(user -> eventPublisher.publishUserDeactivated(user, "Eliminación lógica")
                .thenReturn(user));
    }
}
