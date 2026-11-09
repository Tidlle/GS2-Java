package br.com.zela.api.excecao;

/** Pedido bem formado, mas que fere uma regra do Zela, como check-in longe do local (vira HTTP 422). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
