package br.com.zela.api.excecao;

/** Login bloqueado temporariamente por excesso de senhas erradas (vira HTTP 429). */
public class MuitasTentativasException extends RuntimeException {

    public MuitasTentativasException(long minutos) {
        super("Muitas tentativas de login. Tente de novo em %d minuto(s).".formatted(minutos));
    }
}
