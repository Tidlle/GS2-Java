package br.com.zela.api.servico;

import br.com.zela.api.armazenamento.ArmazenamentoFotos;
import br.com.zela.api.armazenamento.Imagem;
import br.com.zela.api.dominio.Categoria;
import br.com.zela.api.ia.Comparacao;
import br.com.zela.api.ia.ServicoIA;
import br.com.zela.api.ia.Triagem;
import br.com.zela.api.repositorio.CategoriaRepository;
import br.com.zela.api.web.dto.TriagemResposta;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Usa a IA no fluxo do Zela (não é um chat solto):
 * <ul>
 *   <li><b>triagem</b>: a foto vira categoria, descrição e alerta de urgência, que definem executor, prazo e caminho;</li>
 *   <li><b>comparação</b>: antes × depois gera a confiança que alimenta a validação e o aceite tácito.</li>
 * </ul>
 * A resposta da IA é sempre conferida (categoria precisa existir, confiança entre 0 e 1). Se a IA falhar,
 * o fluxo segue com decisão humana: o cidadão escolhe a categoria e o gestor valida a OS.
 */
@Service
public class TriagemService {

    private static final Logger log = LoggerFactory.getLogger(TriagemService.class);
    /** Abaixo disso a sugestão vem marcada para o cidadão revisar. */
    static final double CONFIANCA_MINIMA = 0.5;

    private final ServicoIA ia;
    private final ArmazenamentoFotos fotos;
    private final CategoriaRepository categorias;

    public TriagemService(ServicoIA ia, ArmazenamentoFotos fotos, CategoriaRepository categorias) {
        this.ia = ia;
        this.fotos = fotos;
        this.categorias = categorias;
    }

    /** Salva a foto e devolve a sugestão da IA (o cidadão confirma ou ajusta antes de enviar o reporte). */
    @Transactional(readOnly = true)
    public TriagemResposta triar(MultipartFile arquivo) {
        Imagem foto = fotos.ler(arquivo);
        String url = fotos.salvar(arquivo);
        List<Categoria> todas = categorias.findAll();

        Triagem t;
        try {
            t = ia.triar(foto, todas.stream().map(Categoria::getNome).toList());
        } catch (RuntimeException e) {
            log.warn("Triagem por IA indisponível: {}", motivo(e));
            return TriagemResposta.semSugestao(url, ia.origem(),
                    "A IA não conseguiu analisar a foto agora. Escolha a categoria e descreva o problema.");
        }

        Optional<Categoria> categoria = encontrar(t.categoria(), todas);
        if (categoria.isEmpty()) {
            return TriagemResposta.semSugestao(url, ia.origem(),
                    "A IA não reconheceu um problema de zeladoria na foto. Escolha a categoria manualmente.");
        }
        double confianca = limitar(t.confianca());
        String aviso = confianca < CONFIANCA_MINIMA ? "Sugestão com baixa confiança: confira a categoria." : null;
        if (t.urgente()) {
            aviso = "Risco à vida: ligue 199 (Defesa Civil) ou 193 (Bombeiros). A ocorrência vai direto ao plantão.";
        }
        return new TriagemResposta(url, categoria.get().getId(), categoria.get().getNome(), limparDescricao(t.descricao()),
                t.urgente(), confianca, ia.origem(), aviso);
    }

    /**
     * Compara o antes e o depois. Devolve vazio quando não há foto do antes ou a IA falhou:
     * nesse caso a OS fica para validação humana (gestor ou cidadão), sem aceite tácito.
     */
    public Optional<Comparacao> comparar(String fotoAntesUrl, String fotoDepoisUrl, String descricao) {
        Optional<Imagem> antes = fotos.carregarPorUrl(fotoAntesUrl);
        Optional<Imagem> depois = fotos.carregarPorUrl(fotoDepoisUrl);
        if (antes.isEmpty() || depois.isEmpty()) {
            return Optional.empty();
        }
        try {
            Comparacao c = ia.comparar(antes.get(), depois.get(), descricao);
            return Optional.of(new Comparacao(c.resolvido(), limitar(c.confianca()), c.observacao()));
        } catch (RuntimeException e) {
            log.warn("Comparação por IA indisponível: {}", motivo(e));
            return Optional.empty();
        }
    }

    public String origem() {
        return ia.origem();
    }

    /** Mensagem da causa original (ex.: "API key not valid"), útil para diagnosticar a chave. */
    private static String motivo(Throwable e) {
        Throwable raiz = e;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        String msg = raiz.getMessage() == null ? raiz.getClass().getSimpleName() : raiz.getMessage();
        return msg.length() > 300 ? msg.substring(0, 300) : msg;
    }

    /** Aceita "iluminacao", "ILUMINAÇÃO" etc.: compara sem acento e sem diferenciar maiúsculas. */
    static Optional<Categoria> encontrar(String nome, List<Categoria> todas) {
        if (nome == null || nome.isBlank()) {
            return Optional.empty();
        }
        String alvo = normalizar(nome);
        return todas.stream().filter(c -> normalizar(c.getNome()).equals(alvo)).findFirst();
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    static double limitar(double confianca) {
        double c = Double.isNaN(confianca) ? 0 : Math.clamp(confianca, 0.0, 1.0);
        return Math.round(c * 100) / 100.0;
    }

    private static String limparDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return "";
        }
        String d = descricao.trim();
        return d.length() > 1000 ? d.substring(0, 1000) : d;
    }
}
