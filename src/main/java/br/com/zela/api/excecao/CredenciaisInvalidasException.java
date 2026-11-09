package br.com.zela.api.excecao;

/** Login recusado. A mensagem não diz se o erro foi no e-mail ou na senha (vira HTTP 401). */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos");
    }
}
