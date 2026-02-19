package pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.response;

import lombok.*;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserRole;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserStatus;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserResponse {

    private String id;
    private String institutionId;
    private String firstName;
    private String lastName;
    private String documentType;
    private String documentNumber;
    private String phone;
    private String address;
    private String email;
    private String userName;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
