package br.com.zela.api.dominio;

import br.com.zela.api.excecao.TransicaoInvalidaException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Ordem de serviço emitida para um executor a partir de uma ocorrência.
 * Toda mudança de status passa por {@link #transicionar}, que valida o ciclo e grava a timeline (auditoria).
 */
@Entity
@Table(name = "ordens_servico")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocorrencia_id", nullable = false)
    private Ocorrencia ocorrencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "executor_id", nullable = false)
    private Executor executor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOS status;

    /** Prazo da etapa atual. Se passar, a OS é escalada ao supervisor. */
    @Column(name = "prazo_etapa", nullable = false)
    private Instant prazoEtapa;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "checkin_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "checkin_longitude"))
    })
    private Localizacao checkin;

    @Column(name = "foto_depois_url", length = 300)
    private String fotoDepoisUrl;

    @Column(length = 500)
    private String observacao;

    /** Confiança da IA (0 a 1) de que o problema foi resolvido, comparando antes e depois. */
    @Column(name = "confianca_ia")
    private Double confiancaIa;

    @Column(name = "protocolo_externo", length = 60)
    private String protocoloExterno;

    /** true se a OS já foi reprovada ou contestada alguma vez (conta como retrabalho do executor). */
    @Column(nullable = false)
    private boolean retrabalho;

    /** Resposta do cidadão: true confirmou, false contestou, vazio ainda não respondeu. */
    @Column(name = "cidadao_confirmou")
    private Boolean cidadaoConfirmou;

    @Column(name = "concluida_em")
    private Instant concluidaEm;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @OneToMany(mappedBy = "ordem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("data ASC, id ASC")
    private List<EventoOS> eventos = new ArrayList<>();

    /** Emite a OS (status EMITIDA) e registra o primeiro evento da timeline. */
    public OrdemServico(Ocorrencia ocorrencia, Executor executor, Duration prazo, Ator ator) {
        this.ocorrencia = ocorrencia;
        this.executor = executor;
        this.status = StatusOS.EMITIDA;
        this.prazoEtapa = Instant.now().plus(prazo);
        this.eventos.add(new EventoOS(this, null, StatusOS.EMITIDA, ator, null));
    }

    /** Muda o status validando o ciclo da OS e registra o evento; lança 409 se a transição não for permitida. */
    public void transicionar(StatusOS novo, Ator ator, String justificativa) {
        if (!status.podeIrPara(novo)) {
            throw new TransicaoInvalidaException("OS", status, novo);
        }
        eventos.add(new EventoOS(this, status, novo, ator, justificativa));
        status = novo;
        if (novo == StatusOS.CONCLUIDA) {
            concluidaEm = Instant.now();
        }
        if (novo == StatusOS.REABERTA) {
            retrabalho = true;
        }
    }

    /** Registra na timeline uma ação que não muda o status (ex.: prazo alterado, cobrança). */
    public void registrarEvento(Ator ator, String justificativa) {
        eventos.add(new EventoOS(this, null, status, ator, justificativa));
    }

    public void definirPrazo(Duration prazo) {
        this.prazoEtapa = Instant.now().plus(prazo);
    }

    public void registrarCheckin(Localizacao local) {
        this.checkin = local;
    }

    public void registrarConclusao(String fotoDepoisUrl, String observacao, Double confiancaIa) {
        this.fotoDepoisUrl = fotoDepoisUrl;
        this.observacao = observacao;
        this.confiancaIa = confiancaIa;
    }

    public void registrarRespostaDoCidadao(boolean confirmou) {
        this.cidadaoConfirmou = confirmou;
    }

    public void reatribuir(Executor novoExecutor) {
        this.executor = novoExecutor;
    }

    public void registrarProtocoloExterno(String protocolo) {
        this.protocoloExterno = protocolo;
    }

    public boolean isAtrasada() {
        return status.isAberta() && prazoEtapa.isBefore(Instant.now());
    }

    public List<EventoOS> getEventos() {
        return Collections.unmodifiableList(eventos);
    }

    @PrePersist
    void aoCriar() {
        if (criadaEm == null) {
            criadaEm = Instant.now();
        }
    }
}
