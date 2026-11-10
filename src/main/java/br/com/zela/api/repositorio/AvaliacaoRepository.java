package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.Avaliacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByOrdemExecutorId(Long executorId);

    /** Por executor: [id do executor, média das notas, quantidade de notas]. */
    @Query("""
            select a.ordem.executor.id, avg(a.nota), count(a.nota) from Avaliacao a
            where a.nota is not null group by a.ordem.executor.id
            """)
    List<Object[]> mediaDeNotasPorExecutor();
}
