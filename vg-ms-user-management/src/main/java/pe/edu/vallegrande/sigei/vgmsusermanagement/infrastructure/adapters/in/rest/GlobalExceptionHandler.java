package pe.edu.vallegrande.sigei.vgmsusermanagement.infrastructure.adapters.in.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.common.ErrorResponse;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions.ConflictException;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions.DomainException;
import pe.edu.vallegrande.sigei.vgmsusermanagement.domain.exceptions.NotFoundException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFoundException(NotFoundException ex,
                                                                       ServerWebExchange exchange) {
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(ConflictException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConflictException(ConflictException ex,
                                                                       ServerWebExchange exchange) {
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex,
                                                                         ServerWebExchange exchange) {
        Map<String, String> details = new HashMap<>();
        ex.getFieldErrors().forEach(fieldError -> details.put(fieldError.getField(), fieldError.getDefaultMessage()));
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            "Error de validación en los campos",
            exchange.getRequest().getPath().value(),
            details);
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    @ExceptionHandler(DomainException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDomainException(DomainException ex, ServerWebExchange exchange) {
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            "Domain Error",
            ex.getMessage(),
            exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex, ServerWebExchange exchange) {
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "Error interno del servidor",
            exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
}

