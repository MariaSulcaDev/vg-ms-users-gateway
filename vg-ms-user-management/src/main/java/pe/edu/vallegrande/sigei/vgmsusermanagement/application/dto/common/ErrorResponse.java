package pe.edu.vallegrande.sigei.vgmsusermanagement.application.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> details;

    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
            .status(status)
            .error(error)
            .message(message)
            .path(path)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static ErrorResponse of(int status, String error, String message, String path,
                                   Map<String, String> details) {
        return ErrorResponse.builder()
            .status(status)
            .error(error)
            .message(message)
            .path(path)
            .timestamp(LocalDateTime.now())
            .details(details)
            .build();
    }
}
