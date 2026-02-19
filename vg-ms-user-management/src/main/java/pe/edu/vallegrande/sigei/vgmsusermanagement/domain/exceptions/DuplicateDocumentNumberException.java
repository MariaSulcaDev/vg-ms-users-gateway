package pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions;

public class DuplicateDocumentNumberException extends ConflictException {

    public DuplicateDocumentNumberException(String documentNumber) {
        super("Ya existe un usuario con el número de documento: " + documentNumber);
    }
}
