package pe.edu.vallegrande.sigei.vgmsusermanagement.application.events;

public record UserDeactivatedEvent(
    String userId,
    String institutionId,
    String reason) {
}
