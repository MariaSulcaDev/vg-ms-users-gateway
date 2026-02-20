package pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserRole;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UpdateUserRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 100)
    private String motherLastName;

    @Size(max = 20)
    private String documentType;

    @Size(max = 15)
    private String documentNumber;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 100)
    private String userName;

    private UserRole role;
}
