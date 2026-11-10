package br.com.zela.api.agendamento;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Roda o ciclo de prazos periodicamente (padrão: a cada 5 minutos; AGENDAMENTO_INTERVALO muda).
 * Desligado nos testes, que disparam o ciclo pelo endpoint para não depender do relógio.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "zela.agendamento.habilitado", havingValue = "true", matchIfMissing = true)
public class AgendadorDePrazos {

    private final CicloDePrazos ciclo;

    public AgendadorDePrazos(CicloDePrazos ciclo) {
        this.ciclo = ciclo;
    }

    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "${zela.agendamento.intervalo:PT5M}")
    public void verificarPrazos() {
        ciclo.executar();
    }
}
