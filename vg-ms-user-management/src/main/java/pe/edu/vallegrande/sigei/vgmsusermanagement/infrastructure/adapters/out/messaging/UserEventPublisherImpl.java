package pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.adapters.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigei.vgmsusermanagement.application.events.UserCreatedEvent;
import pe.edu.vallegrande.sigei.vgmsusermanagement.application.events.UserDeactivatedEvent;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IUserEventPublisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisherImpl implements IUserEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final String EXCHANGE = "sigei.events";

    @Override
    public Mono<Void> publishUserCreated(User user) {
        return Mono.fromRunnable(() -> {
            try {
                UserCreatedEvent event = new UserCreatedEvent(
                    user.getId(),
                    user.getInstitutionId(),
                    user.getRole().name(),
                    user.getFirstName() + " " + user.getLastName());
                String json = objectMapper.writeValueAsString(event);
                rabbitTemplate.convertAndSend(EXCHANGE, "user.created", json);
                log.info("Evento user.created publicado para userId={}", user.getId());
            } catch (Exception e) {
                log.error("Error publicando evento user.created: {}", e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> publishUserDeactivated(User user, String reason) {
        return Mono.fromRunnable(() -> {
            try {
                UserDeactivatedEvent event = new UserDeactivatedEvent(
                    user.getId(),
                    user.getInstitutionId(),
                    reason);
                String json = objectMapper.writeValueAsString(event);
                rabbitTemplate.convertAndSend(EXCHANGE, "user.deactivated", json);
                log.info("Evento user.deactivated publicado para userId={}", user.getId());
            } catch (Exception e) {
                log.error("Error publicando evento user.deactivated: {}", e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
