package pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.adapters.in.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.common.ApiResponse;
import pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.request.CreateUserRequest;
import pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.request.UpdateUserRequest;
import pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.response.UserResponse;
import pe.edu.vallegrande.sigei.vgmsusermanagement.application.mappers.UserMapper;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.User;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserRole;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.models.vo.UserStatus;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.ports.in.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserRest {

    private final ICreateUserUseCase createUserUseCase;
    private final IGetUserUseCase getUserUseCase;
    private final IUpdateUserUseCase updateUserUseCase;
    private final IDeleteUserUseCase deleteUserUseCase;
    private final IRestoreUserUseCase restoreUserUseCase;
    private final UserMapper mapper;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<UserResponse>>>> findAll() {
        return getUserUseCase.findAll()
            .map(mapper::toResponse)
            .collectList()
            .map(list -> ResponseEntity.ok(ApiResponse.success(list, "Usuarios obtenidos")));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> findById(@PathVariable String id) {
        return getUserUseCase.findById(id)
            .map(mapper::toResponse)
            .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Usuario encontrado")));
    }

    @GetMapping("/status/{status}")
    public Mono<ResponseEntity<ApiResponse<List<UserResponse>>>> findByStatus(@PathVariable UserStatus status) {
        return getUserUseCase.findByStatus(status)
            .map(mapper::toResponse)
            .collectList()
            .map(list -> ResponseEntity.ok(ApiResponse.success(list, "Usuarios por estado")));
    }

    @GetMapping("/role/{role}/status/{status}")
    public Mono<ResponseEntity<ApiResponse<List<UserResponse>>>> findByRoleAndStatus(
        @PathVariable UserRole role,
        @PathVariable UserStatus status) {
        return getUserUseCase.findByRoleAndStatus(role, status)
            .map(mapper::toResponse)
            .collectList()
            .map(list -> ResponseEntity.ok(ApiResponse.success(list, "Usuarios por rol y estado")));
    }

    @GetMapping("/institution/{institutionId}")
    public Mono<ResponseEntity<ApiResponse<List<UserResponse>>>> findByInstitutionId(
        @PathVariable String institutionId) {
        return getUserUseCase.findByInstitutionId(institutionId)
            .map(mapper::toResponse)
            .collectList()
            .map(list -> ResponseEntity.ok(ApiResponse.success(list, "Usuarios por institución")));
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> create(@Valid @RequestBody CreateUserRequest request) {
        return createUserUseCase.execute(mapper.toDomain(request))
            .map(mapper::toResponse)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Usuario creado")));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateUserRequest request) {
        return updateUserUseCase.execute(id, mapper.mapUpdateToDomain(request))
            .map(mapper::toResponse)
            .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Usuario actualizado")));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> delete(@PathVariable String id) {
        return deleteUserUseCase.execute(id)
            .map(mapper::toResponse)
            .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Usuario eliminado")));
    }

    @PatchMapping("/{id}/restore")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> restore(@PathVariable String id) {
        return restoreUserUseCase.execute(id)
            .map(mapper::toResponse)
            .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Usuario restaurado")));
    }

}
