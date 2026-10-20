package br.com.atria.exception;

import br.com.atria.dto.response.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class TratadorGlobalExcecoes {
    private static final Logger LOG = LoggerFactory.getLogger(TratadorGlobalExcecoes.class);

    private final Clock clock;

    public TratadorGlobalExcecoes(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler({RecursoNaoEncontradoException.class, NoResourceFoundException.class})
    ResponseEntity<ErroResponse> naoEncontrado(Exception ex, HttpServletRequest requisicao) {
        String mensagem = ex instanceof RecursoNaoEncontradoException ? ex.getMessage() : "Recurso não encontrado.";
        return responder(HttpStatus.NOT_FOUND, mensagem, requisicao);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest requisicao) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), requisicao);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest requisicao) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, mensagem, requisicao);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest requisicao) {
        return responder(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'.", requisicao);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoInvalido(HttpServletRequest requisicao) {
        return responder(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou em formato inválido.", requisicao);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest requisicao) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " não suportado nesta rota.", requisicao);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> inesperado(Exception ex, HttpServletRequest requisicao) {
        LOG.error("Erro inesperado em {}", requisicao.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente.", requisicao);
    }

    private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem, HttpServletRequest requisicao) {
        ErroResponse corpo = new ErroResponse(OffsetDateTime.now(clock), status.value(),
                status.getReasonPhrase(), mensagem, requisicao.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
