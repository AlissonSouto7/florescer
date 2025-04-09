package com.florescer.product.domain.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.florescer.product.domain.exception.personalizadas.FileStorageException;
import com.florescer.product.domain.exception.personalizadas.InvalidPatchException;
import com.florescer.product.domain.exception.personalizadas.ProductNotFoundException;

import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
		log.error("Erro inesperado", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("Erro interno no servidor", "Entre em contato com o suporte."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
		Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
	            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> b));
	    log.warn("Erro de validação: Campos inválidos -> {}", errors);
	    return ResponseEntity.badRequest()
	            .body(new ApiErrorResponse("Erro de validação", errors.toString()));
	}

	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
		log.warn("Produto não encontrado: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse("Produto não encontrado", ex.getMessage()));
	}
	
	@ExceptionHandler(FileStorageException.class)
	public ResponseEntity<ApiErrorResponse> handleFileStorage(FileStorageException ex) {
		log.error("Erro ao salvar imagem: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("Erro ao salvar imagem", ex.getMessage()));
	}
	
	@ExceptionHandler(InvalidPatchException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidPatch(InvalidPatchException ex) {
		log.warn("Requisição inválida: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse("Requisição inválida", ex.getMessage()));
	}
}