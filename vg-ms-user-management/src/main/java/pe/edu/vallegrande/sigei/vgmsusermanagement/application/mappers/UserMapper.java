package pe.edu.vallegrande.sigei.vgmsusermanagement.application.mappers;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.request.CreateUserRequest;
import pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.request.UpdateUserRequest;
import pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.response.UserResponse;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserStatus;


import java.time.LocalDateTime;

@Component
public class UserMapper {

    public User toDomain(CreateUserRequest request) {
        String generatedUserName = request.getFirstName().trim().toLowerCase()
            + "." + request.getLastName().trim().toLowerCase();

        return User.builder()
            .institutionId(request.getInstitutionId())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .documentType(request.getDocumentType())
            .documentNumber(request.getDocumentNumber())
            .phone(request.getPhone())
            .address(request.getAddress())
            .email(request.getEmail())
            .userName(generatedUserName)
            .role(request.getRole())
            .status(UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public void updateDomain(User user, UpdateUserRequest request) {
        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getDocumentType() != null)
            user.setDocumentType(request.getDocumentType());
        if (request.getDocumentNumber() != null)
            user.setDocumentNumber(request.getDocumentNumber());
        if (request.getPhone() != null)
            user.setPhone(request.getPhone());
        if (request.getAddress() != null)
            user.setAddress(request.getAddress());
        if (request.getEmail() != null)
            user.setEmail(request.getEmail());
        if (request.getUserName() != null)
            user.setUserName(request.getUserName());
        if (request.getRole() != null)
            user.setRole(request.getRole());
        user.setUpdatedAt(LocalDateTime.now());
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .institutionId(user.getInstitutionId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .documentType(user.getDocumentType())
            .documentNumber(user.getDocumentNumber())
            .phone(user.getPhone())
            .address(user.getAddress())
            .email(user.getEmail())
            .userName(user.getUserName())
            .role(user.getRole())
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    public User mapUpdateToDomain(UpdateUserRequest request) {
        return User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .documentType(request.getDocumentType())
            .documentNumber(request.getDocumentNumber())
            .phone(request.getPhone())
            .address(request.getAddress())
            .email(request.getEmail())
            .userName(request.getUserName())
            .role(request.getRole())
            .build();
    }
}

