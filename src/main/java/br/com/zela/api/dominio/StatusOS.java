package br.com.zela.api.dominio;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo da ordem de serviço e as transições permitidas.
 *
 * <pre>
 * EMITIDA → ACEITA → EM_EXECUCAO → CONCLUIDA → VALIDADA → ENCERRADA
 * desvios: RECUSADA (volta ao gestor), ENCAMINHADA (outro órgão), REABERTA (retrabalho), ESCALADA (prazo estourado)
 * </pre>
 *
 * Voltar para EMITIDA significa reatribuição a outro executor. ENCERRADA a partir de uma etapa aberta é cancelamento.
 */
public enum StatusOS {
    EMITIDA,
    ACEITA,
    RECUSADA,
    EM_EXECUCAO,
    CONCLUIDA,
    VALIDADA,
    ENCERRADA,
    REABERTA,
    ESCALADA,
    ENCAMINHADA;

    /** Estados para os quais esta OS pode ir. */
    public Set<StatusOS> proximos() {
        return switch (this) {
            case EMITIDA -> EnumSet.of(ACEITA, RECUSADA, ESCALADA, ENCAMINHADA, EMITIDA, ENCERRADA);
            case ACEITA -> EnumSet.of(EM_EXECUCAO, ESCALADA, EMITIDA, ENCERRADA);
            case RECUSADA -> EnumSet.of(EMITIDA, ENCAMINHADA, ENCERRADA);
            case EM_EXECUCAO -> EnumSet.of(CONCLUIDA, ESCALADA, ENCERRADA);
            case CONCLUIDA -> EnumSet.of(VALIDADA, REABERTA, ESCALADA);
            case VALIDADA -> EnumSet.of(ENCERRADA);
            case REABERTA -> EnumSet.of(EM_EXECUCAO, ESCALADA, EMITIDA, ENCERRADA);
            // Escalada é um alerta de atraso: a OS pode retomar qualquer etapa aberta ou ser reatribuída.
            case ESCALADA -> EnumSet.of(EMITIDA, ACEITA, EM_EXECUCAO, CONCLUIDA, ENCERRADA);
            case ENCERRADA, ENCAMINHADA -> EnumSet.noneOf(StatusOS.class);
        };
    }

    public boolean podeIrPara(StatusOS novo) {
        return proximos().contains(novo);
    }

    /** Etapas com prazo correndo (entram em "atrasadas" e no escalonamento). */
    public boolean isAberta() {
        return EnumSet.of(EMITIDA, ACEITA, EM_EXECUCAO, CONCLUIDA, REABERTA, ESCALADA).contains(this);
    }

    public boolean isFinal() {
        return proximos().isEmpty();
    }
}
