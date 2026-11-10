package br.com.zela.api.excecao;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Tratamento global de erros. Toda resposta de erro segue o padrão ProblemDetail (RFC 9457):
 *
 * <pre>
 * { "type": "https://zela.app/erros/validacao", "title": "Dados inválidos", "status": 400,
 *   "detail": "...", "instance": "/categorias", "timestamp": "...", "erros": [ { "campo": "...", "mensagem": "..." } ] }
 * </pre>
 *
 * Erros inesperados viram 500 sem detalhes internos (stack trace só no log).
 */
@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);
    private static final String BASE_TIPO = "https://zela.app/erros/";

    public record ErroDeCampo(String campo, String mensagem) {
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "nao-encontrado", "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler({ConflitoException.class, TransicaoInvalidaException.class})
    ProblemDetail conflito(RuntimeException ex) {
        return problema(HttpStatus.CONFLICT, "conflito", "Conflito com o estado atual", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "conflito", "Conflito com o estado atual",
                "A operação viola uma regra de integridade dos dados (registro duplicado ou em uso)");
    }

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail regraDeNegocio(RegraNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "regra-de-negocio", "Regra de negócio", ex.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ProblemDetail credenciais(CredenciaisInvalidasException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "nao-autenticado", "Não autenticado", ex.getMessage());
    }

    @ExceptionHandler(MuitasTentativasException.class)
    ProblemDetail muitasTentativas(MuitasTentativasException ex) {
        return problema(HttpStatus.TOO_MANY_REQUESTS, "muitas-tentativas", "Muitas tentativas", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail naoAutenticado(AuthenticationException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "nao-autenticado", "Não autenticado",
                "Envie um token válido no cabeçalho Authorization: Bearer <token> (obtido em POST /auth/login)");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acessoNegado(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "acesso-negado", "Acesso negado",
                "Seu papel não tem permissão para esta operação");
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ProblemDetail semPosse(AcessoNegadoException ex) {
        return problema(HttpStatus.FORBIDDEN, "acesso-negado", "Acesso negado", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return problema(HttpStatus.BAD_REQUEST, "validacao", "Dados inválidos",
                "Valor inválido para o parâmetro '%s'".formatted(ex.getName()));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    /** Bean Validation nos DTOs (@Valid @RequestBody): lista cada campo inválido. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErroDeCampo> erros = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(e -> new ErroDeCampo(e.getField(), e.getDefaultMessage()))
                .toList();
        ProblemDetail p = problema(HttpStatus.BAD_REQUEST, "validacao", "Dados inválidos",
                "Um ou mais campos estão inválidos");
        p.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(p);
    }

    /** Validação em parâmetros de método (@RequestParam, @PathVariable com restrições). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErroDeCampo> erros = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new ErroDeCampo(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        ProblemDetail p = problema(HttpStatus.BAD_REQUEST, "validacao", "Dados inválidos",
                "Um ou mais parâmetros estão inválidos");
        p.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(p);
    }

    /** Padroniza também os erros tratados pelo Spring (JSON malformado, método não suportado, rota inexistente…). */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        if (body instanceof ProblemDetail p) {
            p.setProperty("timestamp", Instant.now());
            if (p.getStatus() == HttpStatus.BAD_REQUEST.value() && "Failed to read request".equals(p.getDetail())) {
                p.setTitle("Dados inválidos");
                p.setDetail("O corpo da requisição não é um JSON válido ou tem tipos incorretos");
            }
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    private ProblemDetail problema(HttpStatus status, String tipo, String titulo, String detalhe) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detalhe);
        p.setType(URI.create(BASE_TIPO + tipo));
        p.setTitle(titulo);
        p.setProperty("timestamp", Instant.now());
        return p;
    }
}
