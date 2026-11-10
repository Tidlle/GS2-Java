package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.Notificacao;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    Page<Notificacao> findByDestinatarioId(Long destinatarioId, Pageable paginacao);

    List<Notificacao> findByDestinatarioIdAndLidaFalse(Long destinatarioId);

    long countByDestinatarioIdAndLidaFalse(Long destinatarioId);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.destinatario.id = :usuarioId and n.lida = false")
    int marcarTodasComoLidas(@Param("usuarioId") Long usuarioId);
}
