package pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models;

import lombok.*;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserRole;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String id;
    private String institutionId;
    private String firstName;
    private String lastName;
    private String motherLastName;
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
