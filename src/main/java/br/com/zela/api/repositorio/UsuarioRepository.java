package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.Papel;
import br.com.zela.api.dominio.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Usuario> findByPapel(Papel papel);

    List<Usuario> findByExecutorId(Long executorId);
}
