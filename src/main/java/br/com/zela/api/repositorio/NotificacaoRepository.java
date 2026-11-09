package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.Notificacao;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    Page<Notificacao> findByDestinatarioId(Long destinatarioId, Pageable paginacao);

    List<Notificacao> findByDestinatarioIdAndLidaFalse(Long destinatarioId);
}
