package com.florescer.auth.exception.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.florescer.auth.exception.custom.EmailAlreadyRegisteredException;
import com.florescer.auth.exception.custom.EmailNotFoundException;

import lombok.extern.log4j.Log4j2;

/**
 * Traduz exceções em respostas HTTP.
 *
 * <p>Um handler para {@code Exception} num {@code @RestControllerAdvice} é
 * consultado antes da resolução padrão do Spring, então sem os handlers
 * específicos abaixo o framework nunca chega a traduzir os erros que já sabe
 * traduzir. Medido antes da correção: corpo malformado, corpo ausente, método
 * errado e tipo de conteúdo não suportado devolviam todos 500.
 *
 * <p>Um 500 mente duas vezes: diz ao cliente que não há o que corrigir, e diz a
 * quem opera que existe um defeito onde não existe.
 */
@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        // Nem o log nem o corpo repetem o endereço informado. O serviço já
        // registrou a tentativa com o pseudônimo, que é o que a auditoria usa.
        log.warn("Registro recusado: e-mail já em uso.");
        return status(HttpStatus.CONFLICT, "E-mail já registrado",
                "Não foi possível concluir o cadastro com os dados informados.");
    }

    /**
     * Responde como credencial inválida, e não como "usuário não existe".
     *
     * <p>Distinguir os dois casos informa a quem pergunta se um endereço tem
     * conta, o que permite montar lista de clientes a partir de tentativas de
     * login.
     */
    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailNotFound(EmailNotFoundException ex) {
        log.warn("Falha de autenticação: credenciais não conferem.");
        return status(HttpStatus.UNAUTHORIZED, "Autenticação falhou", "E-mail ou senha incorretos");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Falha de autenticação: credenciais não conferem.");
        return status(HttpStatus.UNAUTHORIZED, "Autenticação falhou", "E-mail ou senha incorretos");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                        (a, b) -> b, LinkedHashMap::new));
        // Só os nomes dos campos vão para o log: as mensagens podem repetir o
        // valor recebido, e o corpo do registro carrega dado pessoal.
        log.warn("Erro de validação: campos inválidos -> {}", errors.keySet());
        // Objeto JSON de verdade, e não o toString de um Map do Java, que
        // nenhum cliente consegue interpretar.
        return ResponseEntity.badRequest().body(new ApiErrorResponse("Erro de validação", errors));
    }

    /** JSON malformado, tipo incompatível no corpo, ou corpo ausente. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Corpo da requisição ilegível.");
        return status(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "O corpo enviado não pôde ser lido. Verifique o formato do JSON.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return status(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "O valor informado para '" + ex.getName() + "' não é válido.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return status(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return status(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado", ex.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return status(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato não suportado",
                "Envie o corpo como application/json.");
    }

    /**
     * Levantada pelos controllers para sinalizar um status específico. Sem este
     * handler ela cairia no genérico e viraria 500, o oposto do que quem a
     * lançou pediu.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiErrorResponse("Requisição inválida", ex.getReason()));
    }

    /**
     * Rota que não existe. Sem este handler ela cai no genérico e vira 500, o
     * que transforma um erro do cliente em alarme de erro do servidor e esconde
     * a diferença entre "endereço errado" e "a aplicação quebrou".
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex) {
        log.warn("Rota não encontrada.");
        return status(HttpStatus.NOT_FOUND, "Recurso não encontrado", "O endereço solicitado não existe.");
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorResponse> handleJwt(JwtException ex) {
        // A mensagem original descreve por que o token falhou, o que ajuda quem
        // ataca a ajustar a tentativa. Ao cliente legítimo basta saber que
        // precisa autenticar de novo.
        log.warn("Token inválido ou expirado.");
        return status(HttpStatus.UNAUTHORIZED, "Token inválido ou expirado", "Faça login novamente.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        log.error("Erro inesperado", ex);
        return status(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor",
                "Entre em contato com o suporte.");
    }

    private ResponseEntity<ApiErrorResponse> status(HttpStatus status, String error, Object details) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(error, details));
    }
}
