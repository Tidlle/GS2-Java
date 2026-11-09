package br.com.zela.api.web.dto;

import br.com.zela.api.dominio.Executor;
import br.com.zela.api.dominio.TipoExecutor;

/** Dados cadastrais do executor. Os indicadores de desempenho entram na Fase 8. */
public record ExecutorResposta(Long id, String nome, TipoExecutor tipo, String regiao, String contrato) {

    public static ExecutorResposta de(Executor e) {
        return new ExecutorResposta(e.getId(), e.getNome(), e.getTipo(), e.getRegiao(), e.getContrato());
    }
}
