package br.com.zela.api.agendamento;

import br.com.zela.api.servico.PrazoService;
import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Um ciclo de verificação de prazos: busca o que venceu e trata item a item.
 * Chamado pelo agendador (a cada N minutos) ou pelo endpoint POST /prazos/executar (demonstração).
 */
@Component
public class CicloDePrazos {

    private static final Logger log = LoggerFactory.getLogger(CicloDePrazos.class);

    private final PrazoService prazos;

    public CicloDePrazos(PrazoService prazos) {
        this.prazos = prazos;
    }

    public synchronized ResumoCiclo executar() {
        int[] falhas = {0};
        int seguiram = tratar(prazos.semConfirmacaoNoPrazo(), prazos::seguirSemConfirmacao, "ocorrência", falhas);
        int escaladas = tratar(prazos.comPrazoEstourado(), prazos::escalar, "OS (escalonamento)", falhas);
        int aceites = tratar(prazos.prontasParaAceiteTacito(), prazos::aceitarTacitamente, "OS (aceite tácito)", falhas);
        ResumoCiclo resumo = new ResumoCiclo(Instant.now(), seguiram, escaladas, aceites, falhas[0]);
        if (seguiram + escaladas + aceites + falhas[0] > 0) {
            log.info("Ciclo de prazos: {} seguiram sem confirmação, {} escaladas, {} aceites tácitos, {} falhas",
                    seguiram, escaladas, aceites, falhas[0]);
        }
        return resumo;
    }

    private static int tratar(List<Long> ids, Predicate<Long> acao, String oQue, int[] falhas) {
        int feitos = 0;
        for (Long id : ids) {
            try {
                if (acao.test(id)) {
                    feitos++;
                }
            } catch (RuntimeException e) {
                falhas[0]++;
                log.error("Falha ao tratar prazo da {} {}: {}", oQue, id, e.getMessage());
            }
        }
        return feitos;
    }
}
