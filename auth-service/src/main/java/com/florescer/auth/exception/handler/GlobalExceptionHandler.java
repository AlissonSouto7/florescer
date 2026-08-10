package com.florescer.auth.exception.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.florescer.auth.exception.custom.EmailAlreadyRegisteredException;
import com.florescer.auth.exception.custom.EmailNotFoundException;

import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        // Nem o log nem o corpo repetem o endereço informado. O serviço já
        // registrou a tentativa com o pseudônimo, que é o que a auditoria usa.
        log.warn("Registro recusado: e-mail já em uso.");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("E-mail já registrado",
                        "Não foi possível concluir o cadastro com os dados informados."));
    }

    /**
     * Responde como credencial inválida, e não como "usuário não existe".
     *
     * <p>Distinguir os dois casos entrega a quem pergunta se um endereço tem
     * conta, o que permite montar lista de clientes a partir de tentativas de
     * login. O e-mail também sai da mensagem, porque ela era registrada em log.
     */
    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailNotFoundException(EmailNotFoundException ex) {
        log.warn("Falha de autenticação: credenciais não conferem.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse("Autenticação falhou", "E-mail ou senha incorretos"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Erro de validação: Campos inválidos -> {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("Erro de validação", errors.toString()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Tentativa de login com credenciais inválidas.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse("Autenticação falhou", "E-mail ou senha incorretos"));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtException(JwtException ex) {
        log.warn("Tentativa de acesso com token inválido ou expirado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse("Token inválido ou expirado", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("Erro interno no servidor", "Entre em contato com o suporte."));
    }
}
