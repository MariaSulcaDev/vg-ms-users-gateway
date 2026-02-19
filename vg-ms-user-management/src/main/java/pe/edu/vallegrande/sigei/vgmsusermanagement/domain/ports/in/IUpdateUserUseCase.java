package pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.in;

import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import reactor.core.publisher.Mono;

public interface IUpdateUserUseCase {
    Mono<User> execute(String id, User user);
}
