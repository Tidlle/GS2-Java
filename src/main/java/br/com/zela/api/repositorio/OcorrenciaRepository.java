package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.dominio.StatusOcorrencia;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    Page<Ocorrencia> findByAutorId(Long autorId, Pageable paginacao);

    Page<Ocorrencia> findByStatusIn(Collection<StatusOcorrencia> status, Pageable paginacao);

    List<Ocorrencia> findByStatus(StatusOcorrencia status);

    boolean existsByCategoriaId(Long categoriaId);
}
