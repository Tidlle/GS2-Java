package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByExecutorResponsavelId(Long executorId);
}
