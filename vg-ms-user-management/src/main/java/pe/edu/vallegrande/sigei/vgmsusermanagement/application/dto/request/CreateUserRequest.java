package pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.Email;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserRole;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreateUserRequest {

    @NotBlank
    private String institutionId;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Size(max = 100)
    private String motherLastName;

    @NotBlank
    @Size(max = 20)
    private String documentType;

    @NotBlank
    @Size(max = 15)
    private String documentNumber;

    @NotBlank
    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    @Email
    @Size(max = 150)
    private String email;

    @NotNull
    private UserRole role;
}
