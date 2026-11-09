package br.com.zela.api.dominio;

import br.com.zela.api.excecao.TransicaoInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Problema reportado pelo cidadão, com foto e localização. Pode virar uma ordem de serviço. */
@Entity
@Table(name = "ocorrencias")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ocorrencia {

    public static final int PRIORIDADE_MAXIMA = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 160)
    private String titulo;

    /** Descrição padronizada sugerida pela IA (e editável pelo cidadão). */
    @Column(name = "descricao_ia", nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false, length = 80)
    private String bairro;

    @Column(nullable = false, length = 200)
    private String endereco;

    @Embedded
    private Localizacao localizacao;

    @Column(name = "foto_antes_url", length = 300)
    private String fotoAntesUrl;

    @Column(nullable = false)
    private boolean urgente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOcorrencia status;

    /** 1 (baixa) a 5 (risco à vida). Confirmações dos vizinhos aumentam a prioridade. */
    @Column(nullable = false)
    private int prioridade;

    /** Contadores das confirmações (os votos individuais ficam em {@link Confirmacao}). */
    @Column(name = "confirmacoes_sim", nullable = false)
    private int confirmacoesSim;

    @Column(name = "confirmacoes_nao", nullable = false)
    private int confirmacoesNao;

    /** OS vigente desta ocorrência (vazia se nunca teve ou se a última foi cancelada). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_atual_id")
    private OrdemServico ordemAtual;

    @Column(name = "orgao_encaminhado", length = 120)
    private String orgaoEncaminhado;

    @Column(name = "protocolo_externo", length = 60)
    private String protocoloExterno;

    @Column(name = "motivo_descarte", length = 500)
    private String motivoDescarte;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    public Ocorrencia(Usuario autor, Categoria categoria, String titulo, String descricao, String bairro,
            String endereco, Localizacao localizacao, String fotoAntesUrl, boolean urgente) {
        this.autor = autor;
        this.categoria = categoria;
        this.titulo = titulo;
        this.descricao = descricao;
        this.bairro = bairro;
        this.endereco = endereco;
        this.localizacao = localizacao;
        this.fotoAntesUrl = fotoAntesUrl;
        this.urgente = urgente;
        this.status = StatusOcorrencia.REPORTADA;
        this.prioridade = urgente ? PRIORIDADE_MAXIMA : categoria.getRiscoBase();
    }

    /** Muda o status validando a transição; lança {@link TransicaoInvalidaException} se não for permitida. */
    public void transicionar(StatusOcorrencia novo) {
        if (!status.podeIrPara(novo)) {
            throw new TransicaoInvalidaException("Ocorrência", status, novo);
        }
        status = novo;
    }

    /** Registra o voto de um vizinho; "ainda existe" aumenta a prioridade. */
    public void registrarConfirmacao(boolean aindaExiste) {
        if (aindaExiste) {
            confirmacoesSim++;
            prioridade = Math.min(PRIORIDADE_MAXIMA, prioridade + 1);
        } else {
            confirmacoesNao++;
        }
    }

    /** Edição pelo autor, permitida só enquanto a ocorrência não virou OS. */
    public void editar(Categoria categoria, String titulo, String descricao, String endereco) {
        this.categoria = categoria;
        this.titulo = titulo;
        this.descricao = descricao;
        this.endereco = endereco;
    }

    public boolean podeSerEditadaPeloAutor() {
        return ordemAtual == null && !status.isFinal() && status != StatusOcorrencia.EM_ATENDIMENTO;
    }

    public void vincularOrdem(OrdemServico ordem) {
        this.ordemAtual = ordem;
    }

    public void desvincularOrdem() {
        this.ordemAtual = null;
    }

    public void encaminhar(String orgao, String protocolo) {
        transicionar(StatusOcorrencia.ENCAMINHADA);
        this.orgaoEncaminhado = orgao;
        this.protocoloExterno = protocolo;
    }

    public void descartar(String motivo) {
        transicionar(StatusOcorrencia.DESCARTADA);
        this.motivoDescarte = motivo;
    }

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        if (criadaEm == null) {
            criadaEm = agora;
        }
        atualizadaEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadaEm = Instant.now();
    }
}
