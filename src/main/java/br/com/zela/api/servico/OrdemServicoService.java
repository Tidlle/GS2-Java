package br.com.zela.api.servico;

import br.com.zela.api.config.ZelaProperties;
import br.com.zela.api.dominio.Ator;
import br.com.zela.api.dominio.Avaliacao;
import br.com.zela.api.dominio.Executor;
import br.com.zela.api.dominio.Localizacao;
import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.dominio.OrdemServico;
import br.com.zela.api.dominio.Papel;
import br.com.zela.api.dominio.StatusOS;
import br.com.zela.api.dominio.StatusOcorrencia;
import br.com.zela.api.dominio.Usuario;
import br.com.zela.api.excecao.AcessoNegadoException;
import br.com.zela.api.excecao.ConflitoException;
import br.com.zela.api.excecao.RecursoNaoEncontradoException;
import br.com.zela.api.excecao.RegraNegocioException;
import br.com.zela.api.ia.Comparacao;
import br.com.zela.api.repositorio.AvaliacaoRepository;
import br.com.zela.api.repositorio.OrdemServicoRepository;
import br.com.zela.api.seguranca.UsuarioLogado;
import br.com.zela.api.web.dto.AvaliacaoRequisicao;
import br.com.zela.api.web.dto.ConclusaoRequisicao;
import br.com.zela.api.web.dto.EncaminhamentoRequisicao;
import br.com.zela.api.web.dto.OrdemResposta;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Ciclo da ordem de serviço:
 * EMITIDA → ACEITA → EM_EXECUCAO (check-in no local) → CONCLUIDA (foto do depois) → VALIDADA → ENCERRADA.
 * Cada ação confere o papel (no controller) e a posse (aqui): o executor só mexe nas OS da sua equipe
 * e o cidadão só vê e avalia as OS das próprias ocorrências.
 */
@Service
public class OrdemServicoService {

    /** Prazo para refazer o serviço depois de reprovação ou contestação. */
    static final Duration PRAZO_RETRABALHO = Duration.ofHours(24);

    private final OrdemServicoRepository ordens;
    private final AvaliacaoRepository avaliacoes;
    private final ExecutorService executores;
    private final UsuarioLogado logado;
    private final ZelaProperties propriedades;
    private final TriagemService ia;

    public OrdemServicoService(OrdemServicoRepository ordens, AvaliacaoRepository avaliacoes,
            ExecutorService executores, UsuarioLogado logado, ZelaProperties propriedades, TriagemService ia) {
        this.ordens = ordens;
        this.avaliacoes = avaliacoes;
        this.executores = executores;
        this.logado = logado;
        this.propriedades = propriedades;
        this.ia = ia;
    }

    // ---------- Consultas ----------

    /** Painel do gestor e do supervisor, com filtros combináveis. */
    @Transactional(readOnly = true)
    public Page<OrdemResposta> listar(StatusOS status, Long executorId, String bairro, Boolean atrasadas,
            Pageable paginacao) {
        Specification<OrdemServico> filtro = (raiz, consulta, cb) -> cb.conjunction();
        if (status != null) {
            filtro = filtro.and((raiz, consulta, cb) -> cb.equal(raiz.get("status"), status));
        }
        if (executorId != null) {
            filtro = filtro.and((raiz, consulta, cb) -> cb.equal(raiz.get("executor").get("id"), executorId));
        }
        if (StringUtils.hasText(bairro)) {
            String termo = "%" + bairro.trim().toLowerCase() + "%";
            filtro = filtro.and((raiz, consulta, cb) -> cb.like(cb.lower(raiz.get("ocorrencia").get("bairro")), termo));
        }
        if (Boolean.TRUE.equals(atrasadas)) {
            Instant agora = Instant.now();
            filtro = filtro.and((raiz, consulta, cb) -> cb.and(
                    raiz.get("status").in(abertas()),
                    cb.lessThan(raiz.get("prazoEtapa"), agora)));
        }
        return ordens.findAll(filtro, paginacao).map(OrdemResposta::resumo);
    }

    /** OS da equipe do executor logado. */
    @Transactional(readOnly = true)
    public Page<OrdemResposta> minhas(StatusOS status, Pageable paginacao) {
        Long executorId = executorDoUsuario(logado.usuario()).getId();
        Page<OrdemServico> pagina = status != null
                ? ordens.findByExecutorIdAndStatus(executorId, status, paginacao)
                : ordens.findByExecutorId(executorId, paginacao);
        return pagina.map(OrdemResposta::resumo);
    }

    /** Detalhe com timeline. Executor: só da sua equipe. Cidadão: só das suas ocorrências. */
    @Transactional(readOnly = true)
    public OrdemResposta buscar(Long id) {
        OrdemServico os = carregar(id);
        Usuario usuario = logado.usuario();
        if (usuario.getPapel() == Papel.EXECUTOR) {
            garantirExecutorDaOS(usuario, os);
        } else if (usuario.getPapel() == Papel.CIDADAO
                && !os.getOcorrencia().getAutor().getId().equals(usuario.getId())) {
            throw new AcessoNegadoException("Esta OS não é de uma ocorrência sua");
        }
        return OrdemResposta.detalhe(os);
    }

    /** Escalonamentos: OS escaladas ou com o prazo da etapa vencido, as mais atrasadas primeiro. */
    @Transactional(readOnly = true)
    public List<OrdemResposta> escalonadas() {
        return ordens.escalonadas(abertas(), Instant.now()).stream().map(OrdemResposta::resumo).toList();
    }

    // ---------- Executor ----------

    @Transactional
    public OrdemResposta aceitar(Long id) {
        Usuario executor = logado.usuario();
        OrdemServico os = daEquipe(id, executor);
        os.transicionar(StatusOS.ACEITA, Ator.de(executor), null);
        return OrdemResposta.detalhe(os);
    }

    /** Recusa com motivo: a OS volta ao gestor, que reatribui ou encaminha a outro órgão. */
    @Transactional
    public OrdemResposta recusar(Long id, String motivo) {
        Usuario executor = logado.usuario();
        OrdemServico os = daEquipe(id, executor);
        os.transicionar(StatusOS.RECUSADA, Ator.de(executor), motivo.trim());
        return OrdemResposta.detalhe(os);
    }

    /** Check-in com GPS: só libera a execução se o executor estiver a até N metros do local da ocorrência. */
    @Transactional
    public OrdemResposta checkin(Long id, double latitude, double longitude) {
        Usuario executor = logado.usuario();
        OrdemServico os = daEquipe(id, executor);
        Localizacao aqui = new Localizacao(latitude, longitude);
        long distancia = Math.round(aqui.distanciaMetros(os.getOcorrencia().getLocalizacao()));
        int raio = propriedades.regras().raioCheckinMetros();
        if (distancia > raio) {
            throw new RegraNegocioException("Você está a %d m do local; o check-in exige no máximo %d m"
                    .formatted(distancia, raio));
        }
        os.transicionar(StatusOS.EM_EXECUCAO, Ator.de(executor), "Check-in a %d m do local".formatted(distancia));
        os.registrarCheckin(aqui);
        return OrdemResposta.detalhe(os);
    }

    /**
     * Conclusão com a foto do depois. A IA compara com a foto do antes e grava a confiança de que o problema
     * foi resolvido (base do aceite tácito). Sem foto do antes ou com a IA fora, a validação fica com pessoas.
     */
    @Transactional
    public OrdemResposta concluir(Long id, ConclusaoRequisicao req) {
        Usuario executor = logado.usuario();
        OrdemServico os = daEquipe(id, executor);
        String fotoDepois = req.fotoDepoisUrl().trim();
        Ocorrencia o = os.getOcorrencia();
        Optional<Comparacao> comparacao = ia.comparar(o.getFotoAntesUrl(), fotoDepois, o.getDescricao());

        os.transicionar(StatusOS.CONCLUIDA, Ator.de(executor), "Foto do depois enviada");
        os.registrarConclusao(fotoDepois, StringUtils.hasText(req.observacao()) ? req.observacao().trim() : null,
                comparacao.map(Comparacao::confianca).orElse(null));
        os.registrarEvento(Ator.sistema("IA (" + ia.origem() + ")"), comparacao
                .map(c -> "%s com %d%% de confiança. %s".formatted(c.resolvido() ? "Resolvido" : "Não resolvido",
                        Math.round(c.confianca() * 100), c.observacao() == null ? "" : c.observacao()).trim())
                .orElse("Comparação antes/depois indisponível; validação pelo gestor ou pelo cidadão"));
        return OrdemResposta.detalhe(os);
    }

    // ---------- Gestor ----------

    @Transactional
    public OrdemResposta aprovar(Long id) {
        OrdemServico os = carregar(id);
        validarEEncerrar(os, Ator.de(logado.usuario()), "Aprovada pelo gestor");
        return OrdemResposta.detalhe(os);
    }

    /** Reprovação: a OS volta ao executor e conta como retrabalho. */
    @Transactional
    public OrdemResposta reprovar(Long id, String motivo) {
        OrdemServico os = carregar(id);
        os.transicionar(StatusOS.REABERTA, Ator.de(logado.usuario()), motivo.trim());
        os.definirPrazo(PRAZO_RETRABALHO);
        return OrdemResposta.detalhe(os);
    }

    @Transactional
    public OrdemResposta alterarPrazo(Long id, int horas) {
        OrdemServico os = carregar(id);
        if (!os.getStatus().isAberta()) {
            throw new ConflitoException("A OS está %s e não tem prazo correndo".formatted(os.getStatus()));
        }
        os.definirPrazo(Duration.ofHours(horas));
        os.registrarEvento(Ator.de(logado.usuario()), "Prazo da etapa alterado para %d h".formatted(horas));
        return OrdemResposta.detalhe(os);
    }

    /** Cancela a OS; a ocorrência volta a CONFIRMADA (fila de exceções) para uma nova emissão. */
    @Transactional
    public OrdemResposta cancelar(Long id, String motivo) {
        OrdemServico os = carregar(id);
        os.transicionar(StatusOS.ENCERRADA, Ator.de(logado.usuario()), "Cancelada: " + motivo.trim());
        Ocorrencia o = os.getOcorrencia();
        o.desvincularOrdem();
        o.transicionar(StatusOcorrencia.CONFIRMADA);
        return OrdemResposta.detalhe(os);
    }

    /** Fora da competência: OS e ocorrência vão para outro órgão, com protocolo. */
    @Transactional
    public OrdemResposta encaminhar(Long id, EncaminhamentoRequisicao req) {
        OrdemServico os = carregar(id);
        String protocolo = req.protocolo().trim();
        os.transicionar(StatusOS.ENCAMINHADA, Ator.de(logado.usuario()),
                "Encaminhada a %s · protocolo %s".formatted(req.orgao().trim(), protocolo));
        os.registrarProtocoloExterno(protocolo);
        os.getOcorrencia().encaminhar(req.orgao().trim(), protocolo);
        return OrdemResposta.detalhe(os);
    }

    // ---------- Cidadão ----------

    /** O cidadão que reportou confirma (encerra) ou contesta (reabre, retrabalho) a solução. */
    @Transactional
    public OrdemResposta avaliar(Long id, AvaliacaoRequisicao req) {
        Usuario cidadao = logado.usuario();
        OrdemServico os = carregar(id);
        if (!os.getOcorrencia().getAutor().getId().equals(cidadao.getId())) {
            throw new AcessoNegadoException("Só quem reportou pode avaliar a solução");
        }
        if (os.getStatus() != StatusOS.CONCLUIDA) {
            throw new ConflitoException("Só é possível avaliar uma OS concluída (status atual: %s)"
                    .formatted(os.getStatus()));
        }
        String comentario = StringUtils.hasText(req.comentario()) ? req.comentario().trim() : null;
        avaliacoes.save(new Avaliacao(os, cidadao, req.aprovada(), req.nota(), comentario));
        os.registrarRespostaDoCidadao(req.aprovada());
        if (req.aprovada()) {
            validarEEncerrar(os, Ator.de(cidadao), "Cidadão confirmou a solução");
        } else {
            os.transicionar(StatusOS.REABERTA, Ator.de(cidadao),
                    comentario != null ? comentario : "Cidadão contestou a solução");
            os.definirPrazo(PRAZO_RETRABALHO);
        }
        return OrdemResposta.detalhe(os);
    }

    // ---------- Supervisor ----------

    @Transactional
    public OrdemResposta cobrar(Long id) {
        OrdemServico os = carregar(id);
        os.registrarEvento(Ator.de(logado.usuario()), "Supervisor cobrou o executor");
        return OrdemResposta.detalhe(os);
    }

    /** Troca o executor; a OS volta a EMITIDA com o prazo da categoria (ou de plantão, se urgente). */
    @Transactional
    public OrdemResposta reatribuir(Long id, Long executorId) {
        OrdemServico os = carregar(id);
        Executor novo = executores.carregar(executorId);
        if (novo.getId().equals(os.getExecutor().getId())) {
            throw new RegraNegocioException("A OS já está com " + novo.getNome());
        }
        String anterior = os.getExecutor().getNome();
        os.transicionar(StatusOS.EMITIDA, Ator.de(logado.usuario()),
                "Reatribuída de %s para %s".formatted(anterior, novo.getNome()));
        os.reatribuir(novo);
        Ocorrencia o = os.getOcorrencia();
        int horas = o.isUrgente() ? propriedades.regras().prazoUrgenciaHoras() : o.getCategoria().getPrazoHoras();
        os.definirPrazo(Duration.ofHours(horas));
        return OrdemResposta.detalhe(os);
    }

    // ---------- Apoio ----------

    /** CONCLUIDA → VALIDADA → ENCERRADA e a ocorrência fica RESOLVIDA. Também usado pelo aceite tácito (Fase 8). */
    void validarEEncerrar(OrdemServico os, Ator ator, String justificativa) {
        os.transicionar(StatusOS.VALIDADA, ator, justificativa);
        os.transicionar(StatusOS.ENCERRADA, Ator.sistema(), null);
        os.getOcorrencia().transicionar(StatusOcorrencia.RESOLVIDA);
    }

    OrdemServico carregar(Long id) {
        return ordens.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("OS", id));
    }

    private OrdemServico daEquipe(Long id, Usuario executor) {
        OrdemServico os = carregar(id);
        garantirExecutorDaOS(executor, os);
        return os;
    }

    private void garantirExecutorDaOS(Usuario usuario, OrdemServico os) {
        if (!executorDoUsuario(usuario).getId().equals(os.getExecutor().getId())) {
            throw new AcessoNegadoException("Esta OS é de outro executor");
        }
    }

    private static Executor executorDoUsuario(Usuario usuario) {
        if (usuario.getExecutor() == null) {
            throw new AcessoNegadoException("Usuário sem equipe executora vinculada");
        }
        return usuario.getExecutor();
    }

    private static List<StatusOS> abertas() {
        return Arrays.stream(StatusOS.values()).filter(StatusOS::isAberta).toList();
    }
}
