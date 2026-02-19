package pe.edu.vallegrande.sigei.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

     @GetMapping("/users")
     public Mono<ResponseEntity<Map<String, Object>>> usersFallback() {
          return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                              "status", 503,
                              "message", "El servicio de usuarios no está disponible en este momento",
                              "timestamp", LocalDateTime.now().toString())));
     }
}
