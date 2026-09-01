package com.mobdata.pontocerto.exception;

import com.mobdata.pontocerto.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Erro de validação (@Valid falhou, ex: campo vazio) -> 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(erro ->
                fields.put(erro.getField(), erro.getDefaultMessage())
        );

        ErrorResponseDTO response = new ErrorResponseDTO(
                "Erro de validação",
                null,
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                fields
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Login/senha errados, ou empresa inativa -> 401 Unauthorized
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErrorResponseDTO> handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        ErrorResponseDTO response = new ErrorResponseDTO(
                ex.getMessage(),
                "CREDENCIAIS_INVALIDAS",
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now(),
                null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // Erros de regra de negócio (ex: "Usuário já cadastrado", "Empresa não encontrada") -> 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponseDTO response = new ErrorResponseDTO(
                ex.getMessage(),
                null,
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Erros de estado inválido (ex: "não pode excluir setor com funcionário alocado") -> 409 Conflict
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalState(IllegalStateException ex) {
        ErrorResponseDTO response = new ErrorResponseDTO(
                ex.getMessage(),
                null,
                HttpStatus.CONFLICT.value(),
                LocalDateTime.now(),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // Qualquer outro erro não previsto -> 500 Internal Server Error
    // NUNCA expõe detalhes internos (stack trace) pro cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericError(Exception ex) {
        ex.printStackTrace(); // loga no console pra você conseguir debugar — troque por um logger de verdade quando puder

        ErrorResponseDTO response = new ErrorResponseDTO(
                "Erro interno no servidor",
                null,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now(),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
