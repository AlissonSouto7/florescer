package com.florescer.product.domain.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import com.florescer.product.domain.exception.custom.DatabaseException;
import com.florescer.product.domain.exception.custom.FileStorageException;
import com.florescer.product.domain.exception.custom.InvalidPatchException;
import com.florescer.product.domain.exception.custom.InvalidSettingsException;
import com.florescer.product.domain.exception.custom.ProductNotFoundException;

import lombok.extern.log4j.Log4j2;

/**
 * Traduz exceções em respostas HTTP.
 *
 * <p>Um detalhe de ordem importa aqui: um handler para {@code Exception} num
 * {@code @RestControllerAdvice} é consultado antes do tratamento padrão do
 * Spring. Sem os handlers específicos abaixo, requisição malformada, método
 * errado, parte ausente e arquivo grande demais viravam todos 500, e o cliente
 * recebia "erro interno" para um problema que era dele resolver.
 *
 * <p>O handler genérico continua existindo como último recurso, e continua sem
 * devolver stacktrace: o que é inesperado vai para o log, não para a resposta.
 */
@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
		log.warn("Produto não encontrado: {}", ex.getMessage());
		return status(HttpStatus.NOT_FOUND, "Produto não encontrado", ex.getMessage());
	}

	@ExceptionHandler(InvalidPatchException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidPatch(InvalidPatchException ex) {
		log.warn("Requisição inválida: {}", ex.getMessage());
		return status(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
	}

	/**
	 * Dado da loja recusado depois da limpeza.
	 *
	 * <p>Sai no mesmo formato da Bean Validation, com o mapa de campo para
	 * mensagem, para a tela mostrar o aviso ao lado do campo certo sem precisar
	 * distinguir de onde o erro veio.
	 */
	@ExceptionHandler(InvalidSettingsException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidSettings(InvalidSettingsException ex) {
		log.warn("Dados da loja inválidos: {}", ex.getErrosPorCampo());
		return ResponseEntity.badRequest().body(new ApiErrorResponse("Erro de validação", ex.getErrosPorCampo()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
		Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
						(a, b) -> b, LinkedHashMap::new));
		log.warn("Erro de validação: campos inválidos -> {}", errors);
		// O mapa vai como objeto JSON, e não como o toString de um Map do Java,
		// para o cliente conseguir ler campo a campo.
		return ResponseEntity.badRequest().body(new ApiErrorResponse("Erro de validação", errors));
	}

	/** JSON malformado, valor fora de um enum, tipo incompatível no corpo. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
		log.warn("Corpo da requisição ilegível: {}", ex.getMessage());
		return status(HttpStatus.BAD_REQUEST, "Requisição inválida",
				"O corpo enviado não pôde ser lido. Verifique o formato do JSON e os valores dos campos.");
	}

	/** Parâmetro de rota com tipo errado, como um UUID que não é UUID. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		log.warn("Parâmetro com tipo inválido: {}", ex.getName());
		return status(HttpStatus.BAD_REQUEST, "Requisição inválida",
				"O valor informado para '" + ex.getName() + "' não é válido.");
	}

	@ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
	public ResponseEntity<ApiErrorResponse> handleMissingInput(Exception ex) {
		log.warn("Entrada obrigatória ausente: {}", ex.getMessage());
		return status(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		return status(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado", ex.getMessage());
	}

	/**
	 * Arquivo estático inexistente, tipicamente uma imagem de produto que já foi
	 * removida do disco.
	 *
	 * <p>Sem este handler a exceção caía no genérico e virava 500, dizendo ao
	 * cliente que o servidor tem um defeito quando o recurso é que não existe.
	 * Também escondia a causa: um 500 não distingue "imagem apagada" de "falha
	 * real", e as duas situações exigem investigações diferentes.
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex) {
		log.warn("Recurso estático não encontrado: {}", paraLog(ex.getResourcePath()));
		return status(HttpStatus.NOT_FOUND, "Recurso não encontrado",
				"O arquivo solicitado não existe.");
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
		log.warn("Upload acima do limite: {}", ex.getMessage());
		return status(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo muito grande",
				"O arquivo enviado excede o tamanho máximo permitido.");
	}

	/**
	 * Levantada pelos controllers para sinalizar um status específico. Sem este
	 * handler ela cairia no genérico e viraria 500, exatamente o oposto do que
	 * quem a lançou pediu.
	 */
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
		log.warn("Requisição rejeitada: {} {}", ex.getStatusCode(), ex.getReason());
		return ResponseEntity.status(ex.getStatusCode())
				.body(new ApiErrorResponse("Requisição inválida", ex.getReason()));
	}

	/**
	 * Só alcança quem já se autenticou: para o cliente anônimo, o
	 * {@code ExceptionTranslationFilter} do Spring Security responde 401 antes
	 * de chegar aqui. A distinção importa, porque 403 para quem nunca se
	 * identificou esconde que bastaria enviar credenciais.
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
		return status(HttpStatus.FORBIDDEN, "Acesso negado",
				"Você não tem permissão para acessar este recurso.");
	}

	@ExceptionHandler(FileStorageException.class)
	public ResponseEntity<ApiErrorResponse> handleFileStorage(FileStorageException ex) {
		log.error("Erro ao salvar imagem: {}", ex.getMessage());
		return status(HttpStatus.BAD_REQUEST, "Imagem inválida", ex.getMessage());
	}

	@ExceptionHandler(DatabaseException.class)
	public ResponseEntity<ApiErrorResponse> handleDatabase(DatabaseException ex) {
		log.error("Erro de banco de dados", ex);
		return status(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor",
				"Entre em contato com o suporte.");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
		log.error("Erro inesperado", ex);
		return status(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor",
				"Entre em contato com o suporte.");
	}

	private ResponseEntity<ApiErrorResponse> status(HttpStatus status, String error, Object details) {
		return ResponseEntity.status(status).body(new ApiErrorResponse(error, details));
	}

	/**
	 * Deixa um valor vindo do cliente seguro para ir ao log.
	 *
	 * <p>O caminho do recurso é escrito por quem faz a requisição, e log é
	 * arquivo de linhas: um {@code 
} no meio do valor cria uma linha nova, com
	 * o texto que o cliente quiser. Dá para forjar entrada de auditoria, apagar
	 * o rastro do que veio antes, ou envenenar quem lê o log com ferramenta.
	 *
	 * <p>Corta também o tamanho, porque caminho de mil caracteres não diagnostica
	 * nada e enche o disco de quem guarda log.
	 * <p>Visível no pacote, e não privado, de propósito: a regra é testada
	 * diretamente. Pelo MockMvc não dá para entregar caractere de controle no
	 * caminho (ele não decodifica o %09 como o servidor real decodifica), e um
	 * teste que não consegue exercitar a regra não prova nada sobre ela.
	 */
	static String paraLog(String valor) {
		if (valor == null) {
			return "(vazio)";
		}
		String limpo = valor.replaceAll("[\r\n\t]", "_");
		return limpo.length() <= 200 ? limpo : limpo.substring(0, 200) + "...";
	}
}
