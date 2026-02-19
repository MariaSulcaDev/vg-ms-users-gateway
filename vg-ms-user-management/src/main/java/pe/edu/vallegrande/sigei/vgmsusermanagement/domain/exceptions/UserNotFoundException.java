package pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(String id) {
        super("Usuario no encontrado con id: " + id);
    }
}