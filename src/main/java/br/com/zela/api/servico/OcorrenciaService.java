package br.com.zela.api.servico;

import br.com.zela.api.config.ZelaProperties;
import br.com.zela.api.dominio.Categoria;
import br.com.zela.api.dominio.Localizacao;
import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.dominio.StatusOcorrencia;
import br.com.zela.api.dominio.Usuario;
import br.com.zela.api.excecao.AcessoNegadoException;
import br.com.zela.api.excecao.ConflitoException;
import br.com.zela.api.excecao.RecursoNaoEncontradoException;
import br.com.zela.api.repositorio.ConfirmacaoRepository;
import br.com.zela.api.repositorio.OcorrenciaRepository;
import br.com.zela.api.seguranca.UsuarioLogado;
import br.com.zela.api.web.dto.OcorrenciaEdicao;
import br.com.zela.api.web.dto.OcorrenciaRequisicao;
import br.com.zela.api.web.dto.OcorrenciaResposta;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Reporte e consulta de ocorrências pelo cidadão (o "CRUD" do app). */
@Service
public class OcorrenciaService {

    private static final double METROS_POR_GRAU = 111_320;

    private final OcorrenciaRepository ocorrencias;
    private final ConfirmacaoRepository confirmacoes;
    private final CategoriaService categorias;
    private final EmissaoOrdemService emissao;
    private final UsuarioLogado logado;
    private final ZelaProperties propriedades;

    public OcorrenciaService(OcorrenciaRepository ocorrencias, ConfirmacaoRepository confirmacoes,
            CategoriaService categorias, EmissaoOrdemService emissao, UsuarioLogado logado,
            ZelaProperties propriedades) {
        this.ocorrencias = ocorrencias;
        this.confirmacoes = confirmacoes;
        this.categorias = categorias;
        this.emissao = emissao;
        this.logado = logado;
        this.propriedades = propriedades;
    }

    /**
     * Novo reporte. Urgente: pula a confirmação e emite a OS do plantão na hora.
     * Normal: aguarda os vizinhos confirmarem (a confirmação acelera, mas nunca bloqueia).
     */
    @Transactional
    public OcorrenciaResposta criar(OcorrenciaRequisicao req) {
        Usuario autor = logado.usuario();
        Categoria categoria = categorias.carregar(req.categoriaId());
        String endereco = StringUtils.hasText(req.endereco()) ? req.endereco().trim() : "Localização por GPS";
        String bairro = StringUtils.hasText(autor.getBairro()) ? autor.getBairro() : "Não informado";

        Ocorrencia o = new Ocorrencia(autor, categoria, titulo(categoria, endereco), req.descricao().trim(), bairro,
                endereco, new Localizacao(req.latitude(), req.longitude()), req.fotoAntesUrl(), req.isUrgente());
        ocorrencias.save(o);

        if (o.isUrgente()) {
            o.transicionar(StatusOcorrencia.URGENTE);
            emissao.emitirUrgencia(o);
        } else {
            o.transicionar(StatusOcorrencia.AGUARDANDO_CONFIRMACAO);
        }
        return OcorrenciaResposta.de(o);
    }

    @Transactional
    public OcorrenciaResposta editar(Long id, OcorrenciaEdicao req) {
        Ocorrencia o = doAutorEditavel(id);
        Categoria categoria = categorias.carregar(req.categoriaId());
        String endereco = StringUtils.hasText(req.endereco()) ? req.endereco().trim() : o.getEndereco();
        o.editar(categoria, titulo(categoria, endereco), req.descricao().trim(), endereco);
        return OcorrenciaResposta.de(o);
    }

    @Transactional
    public void excluir(Long id) {
        Ocorrencia o = doAutorEditavel(id);
        confirmacoes.apagarDaOcorrencia(o.getId());
        ocorrencias.delete(o);
    }

    @Transactional(readOnly = true)
    public OcorrenciaResposta buscar(Long id) {
        return OcorrenciaResposta.de(carregar(id));
    }

    @Transactional(readOnly = true)
    public Page<OcorrenciaResposta> minhas(Pageable paginacao) {
        return ocorrencias.findByAutorId(logado.id(), paginacao).map(OcorrenciaResposta::de);
    }

    /**
     * Listagem para o mapa/lista do app. Com latitude e longitude, traz só as que estão no raio,
     * da mais próxima para a mais distante, com a distância calculada. Sem posição, pagina por data.
     */
    @Transactional(readOnly = true)
    public Page<OcorrenciaResposta> listar(Double lat, Double lng, Integer raio, StatusOcorrencia status,
            Pageable paginacao) {
        if (lat == null || lng == null) {
            Page<Ocorrencia> pagina = status != null
                    ? ocorrencias.findByStatus(status, paginacao)
                    : ocorrencias.findAll(paginacao);
            return pagina.map(OcorrenciaResposta::de);
        }
        List<OcorrenciaResposta> proximas = proximas(lat, lng, raio, status);
        int inicio = (int) Math.min(paginacao.getOffset(), proximas.size());
        int fim = Math.min(inicio + paginacao.getPageSize(), proximas.size());
        return new PageImpl<>(proximas.subList(inicio, fim), paginacao, proximas.size());
    }

    List<OcorrenciaResposta> proximas(double lat, double lng, Integer raio, StatusOcorrencia status) {
        int raioM = raio != null ? raio : propriedades.regras().raioPadraoMetros();
        Localizacao centro = new Localizacao(lat, lng);
        double dLat = raioM / METROS_POR_GRAU;
        double dLng = raioM / (METROS_POR_GRAU * Math.max(0.01, Math.cos(Math.toRadians(lat))));
        return ocorrencias.naRegiao(lat - dLat, lat + dLat, lng - dLng, lng + dLng, status).stream()
                .map(o -> OcorrenciaResposta.de(o, (int) Math.round(centro.distanciaMetros(o.getLocalizacao()))))
                .filter(r -> r.distanciaM() <= raioM)
                .sorted(Comparator.comparingInt(OcorrenciaResposta::distanciaM))
                .toList();
    }

    Ocorrencia carregar(Long id) {
        return ocorrencias.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrência", id));
    }

    private Ocorrencia doAutorEditavel(Long id) {
        Ocorrencia o = carregar(id);
        if (!o.getAutor().getId().equals(logado.id())) {
            throw new AcessoNegadoException("Só quem reportou pode alterar esta ocorrência");
        }
        if (!o.podeSerEditadaPeloAutor()) {
            throw new ConflitoException("A ocorrência já virou OS ou foi encerrada (%s) e não pode mais ser alterada"
                    .formatted(o.getStatus()));
        }
        return o;
    }

    private static String titulo(Categoria categoria, String endereco) {
        String t = categoria.getNome() + ": " + endereco;
        return t.length() > 160 ? t.substring(0, 160) : t;
    }
}
