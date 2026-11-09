package br.com.zela.api.excecao;

/** Recurso inexistente (vira HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super("%s %s não encontrado(a)".formatted(recurso, id));
    }
}
