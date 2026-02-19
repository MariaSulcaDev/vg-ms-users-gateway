package pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out;

import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import reactor.core.publisher.Mono;

public interface IUserEventPublisher {
    Mono<Void> publishUserCreated(User user);

    Mono<Void> publishUserDeactivated(User user, String reason);
}
