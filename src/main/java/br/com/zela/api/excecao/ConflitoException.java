package br.com.zela.api.excecao;

/** Operação que conflita com o estado atual dos dados, como e-mail já cadastrado (vira HTTP 409). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
