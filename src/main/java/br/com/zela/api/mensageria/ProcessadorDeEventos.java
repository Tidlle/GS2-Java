package br.com.zela.api.mensageria;

import br.com.zela.api.dominio.Notificacao;
import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.dominio.OrdemServico;
import br.com.zela.api.dominio.Papel;
import br.com.zela.api.dominio.Usuario;
import br.com.zela.api.dominio.evento.EventoDominio;
import br.com.zela.api.repositorio.NotificacaoRepository;
import br.com.zela.api.repositorio.OcorrenciaRepository;
import br.com.zela.api.repositorio.OrdemServicoRepository;
import br.com.zela.api.repositorio.UsuarioRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de quem é avisado de cada evento. Chamado pelos consumidores do RabbitMQ (ou direto, no modo local).
 * Roda em transação própria (REQUIRES_NEW): o evento chega depois do commit da operação original.
 */
@Service
public class ProcessadorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(ProcessadorDeEventos.class);

    private final NotificacaoRepository notificacoes;
    private final OcorrenciaRepository ocorrencias;
    private final OrdemServicoRepository ordens;
    private final UsuarioRepository usuarios;

    public ProcessadorDeEventos(NotificacaoRepository notificacoes, OcorrenciaRepository ocorrencias,
            OrdemServicoRepository ordens, UsuarioRepository usuarios) {
        this.notificacoes = notificacoes;
        this.ocorrencias = ocorrencias;
        this.ordens = ordens;
        this.usuarios = usuarios;
    }

    /** Cria as notificações do app para as pessoas interessadas no evento. Devolve quantas criou. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int notificar(EventoDominio e) {
        Optional<Ocorrencia> ocorrencia = Optional.ofNullable(e.ocorrenciaId()).flatMap(ocorrencias::findById);
        if (ocorrencia.isEmpty()) {
            log.warn("Evento {} ignorado: ocorrência {} não encontrada", e.tipo(), e.ocorrenciaId());
            return 0;
        }
        Ocorrencia o = ocorrencia.get();
        Usuario autor = o.getAutor();
        String titulo = "\"" + o.getTitulo() + "\"";
        String os = "OS #" + e.osId();
        List<Aviso> avisos = new ArrayList<>();

        switch (e.tipo()) {
            case "os.emitida" -> {
                if (e.anterior() == null) {
                    avisos.add(new Aviso(List.of(autor), "Sua ocorrência virou %s: %s.".formatted(os, titulo)));
                    avisos.add(new Aviso(daEquipe(e.osId()), "Nova %s: %s.".formatted(os, o.getTitulo())));
                } else {
                    avisos.add(new Aviso(daEquipe(e.osId()), "%s foi atribuída à sua equipe: %s.".formatted(os, o.getTitulo())));
                }
            }
            case "os.recusada" -> avisos.add(new Aviso(comPapel(Papel.GESTOR),
                    "%s recusada por %s: %s.".formatted(os, e.autor(), e.detalhe())));
            case "os.em_execucao" -> avisos.add(new Aviso(List.of(autor),
                    "A equipe chegou ao local e começou o serviço: %s.".formatted(titulo)));
            case "os.concluida" -> avisos.add(new Aviso(List.of(autor),
                    "O problema %s foi concluído. Confirme a solução.".formatted(titulo)));
            case "os.reaberta" -> avisos.add(new Aviso(daEquipe(e.osId()),
                    "%s reaberta (retrabalho): %s.".formatted(os, e.detalhe())));
            case "os.escalada" -> {
                avisos.add(new Aviso(comPapel(Papel.SUPERVISOR),
                        "Prazo estourado: %s (%s) foi escalada ao supervisor.".formatted(os, o.getTitulo())));
                avisos.add(new Aviso(List.of(autor),
                        "O prazo de %s estourou e a prefeitura foi alertada.".formatted(titulo)));
            }
            case "os.encerrada" -> {
                if ("VALIDADA".equals(e.anterior())) {
                    avisos.add(new Aviso(List.of(autor), "Sua ocorrência %s foi resolvida. Obrigado por ajudar!".formatted(titulo)));
                }
            }
            case "ocorrencia.urgente" -> {
                List<Usuario> coordenacao = new ArrayList<>(comPapel(Papel.SUPERVISOR));
                coordenacao.addAll(comPapel(Papel.GESTOR));
                avisos.add(new Aviso(coordenacao, "URGENTE: %s. OS enviada ao plantão.".formatted(o.getTitulo())));
            }
            case "ocorrencia.confirmada" -> avisos.add(new Aviso(List.of(autor),
                    "Seus vizinhos confirmaram o problema %s.".formatted(titulo)));
            case "ocorrencia.descartada" -> avisos.add(new Aviso(List.of(autor),
                    "Sua ocorrência %s foi descartada: %s.".formatted(titulo, e.detalhe())));
            case "ocorrencia.encaminhada" -> avisos.add(new Aviso(List.of(autor),
                    "Sua ocorrência %s foi encaminhada a %s.".formatted(titulo, e.detalhe())));
            default -> {
                // Demais mudanças (ex.: ACEITA, EM_ATENDIMENTO, RESOLVIDA) já são cobertas por outro aviso.
            }
        }

        int criadas = 0;
        for (Aviso aviso : avisos) {
            for (Usuario destinatario : semRepetir(aviso.para())) {
                notificacoes.save(new Notificacao(destinatario, limitar(aviso.texto())));
                criadas++;
            }
        }
        if (criadas > 0) {
            log.info("Evento {} → {} notificação(ões)", e.tipo(), criadas);
        }
        return criadas;
    }

    /** Alertas operacionais (urgência e escalonamento): ficam em destaque no log para a coordenação. */
    public void alertar(EventoDominio e) {
        switch (e.tipo()) {
            case "os.escalada" -> log.warn("[ALERTA] OS #{} escalada: {}", e.osId(),
                    e.detalhe() != null ? e.detalhe() : "prazo da etapa estourado");
            case "ocorrencia.urgente" -> log.warn("[ALERTA] Ocorrência urgente #{} enviada ao plantão", e.ocorrenciaId());
            default -> {
            }
        }
    }

    private List<Usuario> daEquipe(Long osId) {
        return Optional.ofNullable(osId).flatMap(ordens::findById)
                .map(OrdemServico::getExecutor)
                .map(ex -> usuarios.findByExecutorId(ex.getId()))
                .orElse(List.of());
    }

    private List<Usuario> comPapel(Papel papel) {
        return usuarios.findByPapel(papel);
    }

    private static Collection<Usuario> semRepetir(List<Usuario> lista) {
        Set<Long> vistos = new LinkedHashSet<>();
        return lista.stream().filter(u -> vistos.add(u.getId())).toList();
    }

    private static String limitar(String texto) {
        return texto.length() > 300 ? texto.substring(0, 297) + "..." : texto;
    }

    private record Aviso(List<Usuario> para, String texto) {
    }
}
