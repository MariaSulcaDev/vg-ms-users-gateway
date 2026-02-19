package pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.persistence.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
@Data
public class UserEntity {

    @Id
    private String id;

    @Column("institution_id")
    private String institutionId;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("document_type")
    private String documentType;

    @Column("document_number")
    private String documentNumber;

    @Column("phone")
    private String phone;

    @Column("address")
    private String address;

    @Column("email")
    private String email;

    @Column("user_name")
    private String userName;

    @Column("role")
    private String role;

    @Column("status")
    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
