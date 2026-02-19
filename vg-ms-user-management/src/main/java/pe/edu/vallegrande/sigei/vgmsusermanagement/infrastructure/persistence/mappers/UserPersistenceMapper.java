package pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.persistence.mappers;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserRole;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserStatus;
import pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.persistence.entities.UserEntity;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserEntity entity) {
        return User.builder()
            .id(entity.getId())
            .institutionId(entity.getInstitutionId())
            .firstName(entity.getFirstName())
            .lastName(entity.getLastName())
            .documentType(entity.getDocumentType())
            .documentNumber(entity.getDocumentNumber())
            .phone(entity.getPhone())
            .address(entity.getAddress())
            .email(entity.getEmail())
            .userName(entity.getUserName())
            .role(UserRole.valueOf(entity.getRole()))
            .status(UserStatus.valueOf(entity.getStatus()))
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    public UserEntity toEntity(User domain) {
        return UserEntity.builder()
            .id(domain.getId())
            .institutionId(domain.getInstitutionId())
            .firstName(domain.getFirstName())
            .lastName(domain.getLastName())
            .documentType(domain.getDocumentType())
            .documentNumber(domain.getDocumentNumber())
            .phone(domain.getPhone())
            .address(domain.getAddress())
            .email(domain.getEmail())
            .userName(domain.getUserName())
            .role(domain.getRole().name())
            .status(domain.getStatus().name())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
    }
}
