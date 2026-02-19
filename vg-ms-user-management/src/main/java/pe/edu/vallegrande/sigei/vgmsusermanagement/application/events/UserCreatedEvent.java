package pe.edu.vallegrande.sigei.vgmsusermanagement.application.events;

public record UserCreatedEvent(
    String userId,
    String institutionId,
    String role,
    String fullName) {
}
