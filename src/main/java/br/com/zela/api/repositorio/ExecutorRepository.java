package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.Executor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutorRepository extends JpaRepository<Executor, Long> {

    Page<Executor> findByNomeContainingIgnoreCase(String nome, Pageable paginacao);
}
