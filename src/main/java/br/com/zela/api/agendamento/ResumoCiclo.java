package br.com.zela.api.agendamento;

import java.time.Instant;

/** O que um ciclo de verificação de prazos fez. */
public record ResumoCiclo(Instant executadoEm, int seguiramSemConfirmacao, int escaladas, int aceitesTacitos, int falhas) {
}
