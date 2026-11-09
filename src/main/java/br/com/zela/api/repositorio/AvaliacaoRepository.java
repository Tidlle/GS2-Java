package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.Avaliacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByOrdemExecutorId(Long executorId);
}
