package br.com.zela.api.ia;

/** A IA não respondeu ou respondeu algo inválido. O fluxo continua sem ela (decisão manual). */
public class FalhaIAException extends RuntimeException {

    public FalhaIAException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
