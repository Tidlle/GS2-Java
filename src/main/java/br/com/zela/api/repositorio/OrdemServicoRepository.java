package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.OrdemServico;
import br.com.zela.api.dominio.StatusOS;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long>,
        JpaSpecificationExecutor<OrdemServico> {

    // Os grafos carregam junto o executor, a ocorrência e a categoria (resposta sem consultas extras).

    @EntityGraph(attributePaths = {"executor", "ocorrencia", "ocorrencia.categoria"})
    Page<OrdemServico> findByExecutorId(Long executorId, Pageable paginacao);

    @EntityGraph(attributePaths = {"executor", "ocorrencia", "ocorrencia.categoria"})
    Page<OrdemServico> findByExecutorIdAndStatus(Long executorId, StatusOS status, Pageable paginacao);

    @Override
    @EntityGraph(attributePaths = {"executor", "ocorrencia", "ocorrencia.categoria"})
    Page<OrdemServico> findAll(Specification<OrdemServico> filtro, Pageable paginacao);

    List<OrdemServico> findByStatusIn(Collection<StatusOS> status);

    boolean existsByExecutorId(Long executorId);

    /** Escaladas ou com o prazo da etapa vencido em uma etapa aberta (as mais atrasadas primeiro). */
    @Query("""
            select os from OrdemServico os join fetch os.executor join fetch os.ocorrencia o join fetch o.categoria
            where os.status = br.com.zela.api.dominio.StatusOS.ESCALADA
               or (os.status in :abertas and os.prazoEtapa < :agora)
            order by os.prazoEtapa asc
            """)
    List<OrdemServico> escalonadas(@Param("abertas") Collection<StatusOS> abertas, @Param("agora") Instant agora);
}
