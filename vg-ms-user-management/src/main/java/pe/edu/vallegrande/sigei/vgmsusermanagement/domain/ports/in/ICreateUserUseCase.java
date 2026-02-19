package pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.in;

import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import reactor.core.publisher.Mono;

public interface ICreateUserUseCase {
    Mono<User> execute(User user);
}
