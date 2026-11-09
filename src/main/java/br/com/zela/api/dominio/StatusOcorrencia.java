package br.com.zela.api.dominio;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de uma ocorrência e as transições permitidas entre eles.
 *
 * <pre>
 * REPORTADA → EM_TRIAGEM → AGUARDANDO_CONFIRMACAO → CONFIRMADA → EM_ATENDIMENTO → RESOLVIDA
 * desvios: URGENTE (pula a confirmação), DESCARTADA (negada ou sem fundamento), ENCAMINHADA (outro órgão)
 * </pre>
 */
public enum StatusOcorrencia {
    REPORTADA,
    EM_TRIAGEM,
    AGUARDANDO_CONFIRMACAO,
    CONFIRMADA,
    EM_ATENDIMENTO,
    RESOLVIDA,
    URGENTE,
    DESCARTADA,
    ENCAMINHADA;

    /** Estados para os quais esta ocorrência pode ir. */
    public Set<StatusOcorrencia> proximos() {
        return switch (this) {
            case REPORTADA -> EnumSet.of(EM_TRIAGEM, AGUARDANDO_CONFIRMACAO, URGENTE);
            case EM_TRIAGEM -> EnumSet.of(AGUARDANDO_CONFIRMACAO, CONFIRMADA, EM_ATENDIMENTO, URGENTE,
                    DESCARTADA, ENCAMINHADA);
            case AGUARDANDO_CONFIRMACAO -> EnumSet.of(CONFIRMADA, EM_ATENDIMENTO, DESCARTADA, ENCAMINHADA);
            // Confirmada volta para cá também quando a OS é cancelada.
            case CONFIRMADA -> EnumSet.of(EM_ATENDIMENTO, DESCARTADA, ENCAMINHADA);
            case EM_ATENDIMENTO -> EnumSet.of(RESOLVIDA, CONFIRMADA, ENCAMINHADA);
            // Urgente segue com a OS do plantão até a solução.
            case URGENTE -> EnumSet.of(RESOLVIDA, CONFIRMADA, ENCAMINHADA);
            case RESOLVIDA, DESCARTADA, ENCAMINHADA -> EnumSet.noneOf(StatusOcorrencia.class);
        };
    }

    public boolean podeIrPara(StatusOcorrencia novo) {
        return proximos().contains(novo);
    }

    /** Estados sem saída: a ocorrência acabou. */
    public boolean isFinal() {
        return proximos().isEmpty();
    }
}
