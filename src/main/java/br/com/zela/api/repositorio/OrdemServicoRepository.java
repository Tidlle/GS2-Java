package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.OrdemServico;
import br.com.zela.api.dominio.StatusOS;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    Page<OrdemServico> findByExecutorId(Long executorId, Pageable paginacao);

    List<OrdemServico> findByStatusIn(Collection<StatusOS> status);

    boolean existsByExecutorId(Long executorId);
}
