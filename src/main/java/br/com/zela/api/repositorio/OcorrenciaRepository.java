package br.com.zela.api.repositorio;

import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.dominio.StatusOcorrencia;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    @EntityGraph(attributePaths = "categoria")
    Page<Ocorrencia> findByAutorId(Long autorId, Pageable paginacao);

    @EntityGraph(attributePaths = "categoria")
    Page<Ocorrencia> findByStatus(StatusOcorrencia status, Pageable paginacao);

    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Ocorrencia> findAll(Pageable paginacao);

    List<Ocorrencia> findByStatus(StatusOcorrencia status);

    boolean existsByCategoriaId(Long categoriaId);

    /** Ocorrências dentro de um retângulo de coordenadas (o filtro fino por raio é feito em memória). */
    @Query("""
            select o from Ocorrencia o join fetch o.categoria
            where o.localizacao.latitude between :latMin and :latMax
              and o.localizacao.longitude between :lngMin and :lngMax
              and (:status is null or o.status = :status)
            """)
    List<Ocorrencia> naRegiao(@Param("latMin") double latMin, @Param("latMax") double latMax,
            @Param("lngMin") double lngMin, @Param("lngMax") double lngMax,
            @Param("status") StatusOcorrencia status);

    /** Aguardando confirmação, de outros autores, em que o usuário ainda não votou. */
    @Query("""
            select o from Ocorrencia o join fetch o.categoria
            where o.status = br.com.zela.api.dominio.StatusOcorrencia.AGUARDANDO_CONFIRMACAO
              and o.autor.id <> :usuarioId
              and not exists (select c.id from Confirmacao c where c.ocorrencia = o and c.usuario.id = :usuarioId)
            order by o.criadaEm desc
            """)
    List<Ocorrencia> paraConfirmar(@Param("usuarioId") Long usuarioId);

    /** Por bairro: [bairro, quantidade de ocorrências]. */
    @Query("select o.bairro, count(o) from Ocorrencia o group by o.bairro")
    List<Object[]> contagemPorBairro();

    /** Aguardando confirmação há mais tempo que o limite (seguem sozinhas: a confirmação nunca bloqueia). */
    @Query("""
            select o.id from Ocorrencia o
            where o.status = br.com.zela.api.dominio.StatusOcorrencia.AGUARDANDO_CONFIRMACAO and o.criadaEm < :limite
            """)
    List<Long> idsAguardandoDesde(@Param("limite") Instant limite);

    /** Fila de exceções do gestor: confirmadas (ou em triagem) sem OS e fora do caminho de urgência. */
    @Query(value = """
            select o from Ocorrencia o join fetch o.categoria
            where o.ordemAtual is null and o.urgente = false
              and o.status in (br.com.zela.api.dominio.StatusOcorrencia.CONFIRMADA,
                               br.com.zela.api.dominio.StatusOcorrencia.EM_TRIAGEM)
            order by o.prioridade desc, o.criadaEm asc
            """,
            countQuery = """
            select count(o) from Ocorrencia o
            where o.ordemAtual is null and o.urgente = false
              and o.status in (br.com.zela.api.dominio.StatusOcorrencia.CONFIRMADA,
                               br.com.zela.api.dominio.StatusOcorrencia.EM_TRIAGEM)
            """)
    Page<Ocorrencia> excecoes(Pageable paginacao);
}
