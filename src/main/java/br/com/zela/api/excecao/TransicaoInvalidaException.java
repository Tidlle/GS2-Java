package br.com.zela.api.excecao;

/** Tentativa de mudar o status para um estado não permitido (vira HTTP 409 no tratamento global). */
public class TransicaoInvalidaException extends RuntimeException {

    public TransicaoInvalidaException(String entidade, Enum<?> atual, Enum<?> novo) {
        super("%s não pode passar de %s para %s".formatted(entidade, atual, novo));
    }
}
