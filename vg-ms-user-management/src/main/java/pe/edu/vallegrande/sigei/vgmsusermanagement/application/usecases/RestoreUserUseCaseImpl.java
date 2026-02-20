package pe.edu.vallegrande.sigei.vgmsusermanagement.application.usecases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions.UserNotFoundException;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserStatus;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.in.IRestoreUserUseCase;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IUserRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RestoreUserUseCaseImpl implements IRestoreUserUseCase {

    private final IUserRepository userRepository;

    @Override
    public Mono<User> execute(String id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                .flatMap(user -> {
                    user.setStatus(UserStatus.ACTIVE);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                });
    }
}
