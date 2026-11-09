package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.Confirmacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmacaoRepository extends JpaRepository<Confirmacao, Long> {

    boolean existsByOcorrenciaIdAndUsuarioId(Long ocorrenciaId, Long usuarioId);
}
