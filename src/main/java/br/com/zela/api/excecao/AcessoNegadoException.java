package br.com.zela.api.excecao;

/** O usuário tem o papel certo, mas o recurso não é dele, como editar a ocorrência de outra pessoa (vira HTTP 403). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
