package br.com.zela.api.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.zela.api.dominio.Ator;
import br.com.zela.api.dominio.Confirmacao;
import br.com.zela.api.dominio.EventoOS;
import br.com.zela.api.dominio.Localizacao;
import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.dominio.OrdemServico;
import br.com.zela.api.dominio.Papel;
import br.com.zela.api.dominio.StatusOS;
import br.com.zela.api.dominio.StatusOcorrencia;
import br.com.zela.api.dominio.Usuario;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Confere que o schema do Flyway bate com as entidades e que a carga inicial está coerente. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MapeamentoJpaTest {

    @Autowired UsuarioRepository usuarios;
    @Autowired ExecutorRepository executores;
    @Autowired CategoriaRepository categorias;
    @Autowired OcorrenciaRepository ocorrencias;
    @Autowired OrdemServicoRepository ordens;
    @Autowired ConfirmacaoRepository confirmacoes;
    @Autowired NotificacaoRepository notificacoes;
    @Autowired EntityManager em;

    @Test
    void cargaInicialTemOsCenariosDoApp() {
        assertThat(usuarios.count()).isEqualTo(9);
        assertThat(executores.count()).isEqualTo(5);
        assertThat(categorias.count()).isEqualTo(5);
        assertThat(ocorrencias.count()).isEqualTo(13);
        assertThat(ordens.count()).isEqualTo(9);
        assertThat(usuarios.findByPapel(Papel.CIDADAO)).hasSize(2);
        // Todos os status da OS aparecem na carga, exceto VALIDADA (passagem rápida para ENCERRADA).
        assertThat(ordens.findAll()).extracting(OrdemServico::getStatus).contains(
                StatusOS.EMITIDA, StatusOS.ACEITA, StatusOS.EM_EXECUCAO, StatusOS.CONCLUIDA, StatusOS.REABERTA,
                StatusOS.ESCALADA, StatusOS.ENCAMINHADA, StatusOS.ENCERRADA);
    }

    @Test
    void senhaDeTesteConfereComOHash() {
        Usuario gestor = usuarios.findByEmailIgnoreCase("GESTOR@zela.com").orElseThrow();
        assertThat(gestor.getPapel()).isEqualTo(Papel.GESTOR);
        assertThat(new BCryptPasswordEncoder().matches("123456", gestor.getSenhaHash())).isTrue();
    }

    @Test
    void executorDeTesteEstaLigadoAEmpresaDeIluminacao() {
        Usuario executor = usuarios.findByEmailIgnoreCase("executor@zela.com").orElseThrow();
        assertThat(executor.getExecutor().getNome()).isEqualTo("Equipe Luz Norte");
        assertThat(ordens.findByExecutorId(executor.getExecutor().getId(), PageRequest.of(0, 10)))
                .extracting(OrdemServico::getStatus)
                .containsExactlyInAnyOrder(StatusOS.EMITIDA, StatusOS.ACEITA, StatusOS.CONCLUIDA);
    }

    @Test
    void timelineVemOrdenadaPorData() {
        OrdemServico reaberta = ordens.findById(6L).orElseThrow();
        assertThat(reaberta.isRetrabalho()).isTrue();
        assertThat(reaberta.getEventos()).extracting(EventoOS::getStatusNovo).containsExactly(
                StatusOS.EMITIDA, StatusOS.ACEITA, StatusOS.EM_EXECUCAO, StatusOS.CONCLUIDA, StatusOS.REABERTA);
        assertThat(reaberta.getOcorrencia().getOrdemAtual().getId()).isEqualTo(6L);
    }

    @Test
    void novaOcorrenciaComOsETimelineEhPersistida() {
        Usuario ana = usuarios.findByEmailIgnoreCase("cidadao@zela.com").orElseThrow();
        var iluminacao = categorias.findById(1L).orElseThrow();
        Ocorrencia o = new Ocorrencia(ana, iluminacao, "Poste apagado na R. Teste", "Poste apagado.",
                "Vila Mariana", "R. Teste, 10", new Localizacao(-23.5890, -46.6340), null, false);
        o.transicionar(StatusOcorrencia.AGUARDANDO_CONFIRMACAO);
        o.transicionar(StatusOcorrencia.CONFIRMADA);
        ocorrencias.save(o);

        OrdemServico os = new OrdemServico(o, iluminacao.getExecutorResponsavel(),
                Duration.ofHours(iluminacao.getPrazoHoras()), Ator.sistema("Sistema (emissão automática)"));
        os.transicionar(StatusOS.ACEITA, Ator.de(usuarios.findByEmailIgnoreCase("executor@zela.com").orElseThrow()), null);
        ordens.save(os);
        o.vincularOrdem(os);
        o.transicionar(StatusOcorrencia.EM_ATENDIMENTO);
        em.flush();
        em.clear();

        OrdemServico salva = ordens.findById(os.getId()).orElseThrow();
        assertThat(salva.getId()).isGreaterThanOrEqualTo(100L);
        assertThat(salva.getEventos()).hasSize(2);
        assertThat(salva.getEventos().getLast().getUsuario().getNome()).isEqualTo("Equipe Luz Norte");
        assertThat(salva.getOcorrencia().getStatus()).isEqualTo(StatusOcorrencia.EM_ATENDIMENTO);
        assertThat(salva.getOcorrencia().getCriadaEm()).isNotNull();
        assertThat(salva.getOcorrencia().getLocalizacao().latitude()).isEqualTo(-23.5890);
    }

    @Test
    void mesmoUsuarioNaoVotaDuasVezes() {
        Usuario joao = usuarios.findByEmailIgnoreCase("cidadao2@zela.com").orElseThrow();
        Ocorrencia poste = ocorrencias.findById(1L).orElseThrow();
        assertThat(confirmacoes.existsByOcorrenciaIdAndUsuarioId(1L, joao.getId())).isTrue();

        assertThatThrownBy(() -> {
            confirmacoes.save(new Confirmacao(poste, joao, true));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void notificacoesPorDestinatario() {
        assertThat(notificacoes.findByDestinatarioIdAndLidaFalse(1L)).hasSize(2);
    }
}
