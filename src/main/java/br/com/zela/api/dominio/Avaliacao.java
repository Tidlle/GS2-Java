package br.com.zela.api.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Avaliação do cidadão sobre a solução: aprova ou contesta, com nota opcional de 1 a 5. */
@Entity
@Table(name = "avaliacoes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_id", nullable = false)
    private OrdemServico ordem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cidadao_id", nullable = false)
    private Usuario cidadao;

    @Column(nullable = false)
    private boolean aprovada;

    /** Nota de 1 a 5 para o serviço (opcional). */
    private Integer nota;

    @Column(length = 500)
    private String comentario;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    public Avaliacao(OrdemServico ordem, Usuario cidadao, boolean aprovada, Integer nota, String comentario) {
        this.ordem = ordem;
        this.cidadao = cidadao;
        this.aprovada = aprovada;
        this.nota = nota;
        this.comentario = comentario;
    }

    @PrePersist
    void aoCriar() {
        if (criadaEm == null) {
            criadaEm = Instant.now();
        }
    }
}
