package pe.edu.vallegrande.sigei.vgmsusermanagement.application.usecases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions.DuplicateDocumentNumberException;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions.UserNotFoundException;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.in.IUpdateUserUseCase;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.out.IUserRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UpdateUserUseCaseImpl implements IUpdateUserUseCase {

    private final IUserRepository userRepository;

    @Override
    public Mono<User> execute(String id, User updatedData) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
            .flatMap(existing -> {
                if (updatedData.getDocumentNumber() != null
                    && !updatedData.getDocumentNumber().equals(existing.getDocumentNumber())) {
                    return userRepository.existsByDocumentNumberAndIdNot(updatedData.getDocumentNumber(), id)
                        .flatMap(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                return Mono.<User>error(new DuplicateDocumentNumberException(
                                    updatedData.getDocumentNumber()));
                            }
                            return Mono.just(existing);
                        });
                }
                return Mono.just(existing);
            })
            .flatMap(existing -> {
                mergeFields(existing, updatedData);
                existing.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(existing);
            });
    }

    private void mergeFields(User existing, User updated) {
        if (updated.getFirstName() != null)
            existing.setFirstName(updated.getFirstName());
        if (updated.getLastName() != null)
            existing.setLastName(updated.getLastName());
        if (updated.getDocumentType() != null)
            existing.setDocumentType(updated.getDocumentType());
        if (updated.getDocumentNumber() != null)
            existing.setDocumentNumber(updated.getDocumentNumber());
        if (updated.getPhone() != null)
            existing.setPhone(updated.getPhone());
        if (updated.getAddress() != null)
            existing.setAddress(updated.getAddress());
        if (updated.getEmail() != null)
            existing.setEmail(updated.getEmail());
        if (updated.getUserName() != null)
            existing.setUserName(updated.getUserName());
        if (updated.getRole() != null)
            existing.setRole(updated.getRole());
    }
}

