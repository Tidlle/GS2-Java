package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.Confirmacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfirmacaoRepository extends JpaRepository<Confirmacao, Long> {

    boolean existsByOcorrenciaIdAndUsuarioId(Long ocorrenciaId, Long usuarioId);

    @Modifying
    @Query("delete from Confirmacao c where c.ocorrencia.id = :ocorrenciaId")
    void apagarDaOcorrencia(@Param("ocorrenciaId") Long ocorrenciaId);
}
