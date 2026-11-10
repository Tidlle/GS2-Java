package br.com.zela.api.servico;

import br.com.zela.api.repositorio.NotificacaoRepository;
import br.com.zela.api.seguranca.UsuarioLogado;
import br.com.zela.api.web.dto.NotificacaoResposta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Avisos do usuário logado (criados pelos consumidores do RabbitMQ). */
@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacoes;
    private final UsuarioLogado logado;

    public NotificacaoService(NotificacaoRepository notificacoes, UsuarioLogado logado) {
        this.notificacoes = notificacoes;
        this.logado = logado;
    }

    @Transactional(readOnly = true)
    public Page<NotificacaoResposta> minhas(Pageable paginacao) {
        return notificacoes.findByDestinatarioId(logado.id(), paginacao).map(NotificacaoResposta::de);
    }

    @Transactional(readOnly = true)
    public long naoLidas() {
        return notificacoes.countByDestinatarioIdAndLidaFalse(logado.id());
    }

    @Transactional
    public int marcarTodasComoLidas() {
        return notificacoes.marcarTodasComoLidas(logado.id());
    }
}
