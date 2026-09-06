package br.com.certame.comum;

import br.com.certame.comum.excecao.ErroNegocioException;
import br.com.certame.comum.excecao.MuitasTentativasException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManipuladorErrosGlobal {
    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    @ExceptionHandler(ErroNegocioException.class)
    public ResponseEntity<ErroApi> tratarNegocio(ErroNegocioException excecao) {
        var resposta = ResponseEntity.status(excecao.getStatus()).contentType(PROBLEM_JSON);
        if (excecao instanceof MuitasTentativasException muitas) {
            return resposta.header("Retry-After", Long.toString(muitas.getRetryAfter()))
                    .body(new ErroApi(excecao.getCode(), excecao.getMessage(), excecao.getFieldErrors()));
        }
        return resposta.body(new ErroApi(excecao.getCode(), excecao.getMessage(), excecao.getFieldErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApi> tratarValidacao(MethodArgumentNotValidException excecao) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError erro : excecao.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity.unprocessableContent().contentType(PROBLEM_JSON)
                .body(new ErroApi("DADOS_INVALIDOS", "Verifique os campos destacados.", erros));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroApi> tratarViolacoes(ConstraintViolationException excecao) {
        Map<String, String> erros = new LinkedHashMap<>();
        excecao.getConstraintViolations().forEach(violacao ->
                erros.putIfAbsent(violacao.getPropertyPath().toString(), violacao.getMessage()));
        return ResponseEntity.unprocessableContent().contentType(PROBLEM_JSON)
                .body(new ErroApi("DADOS_INVALIDOS", "Verifique os campos destacados.", erros));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroApi> tratarIntegridade(DataIntegrityViolationException excecao) {
        return ResponseEntity.unprocessableContent().contentType(PROBLEM_JSON)
                .body(new ErroApi("INTEGRIDADE_INVALIDA",
                        "Não foi possível concluir a operação com os dados informados.", null));
    }

}
