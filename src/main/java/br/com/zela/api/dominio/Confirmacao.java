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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Resposta de um vizinho a "Esse problema ainda está aqui?". Cada usuário vota uma vez por ocorrência. */
@Entity
@Table(name = "confirmacoes",
        uniqueConstraints = @UniqueConstraint(name = "uk_confirmacao_usuario",
                columnNames = {"ocorrencia_id", "usuario_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Confirmacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocorrencia_id", nullable = false)
    private Ocorrencia ocorrencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "ainda_existe", nullable = false)
    private boolean aindaExiste;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    public Confirmacao(Ocorrencia ocorrencia, Usuario usuario, boolean aindaExiste) {
        this.ocorrencia = ocorrencia;
        this.usuario = usuario;
        this.aindaExiste = aindaExiste;
    }

    @PrePersist
    void aoCriar() {
        if (criadaEm == null) {
            criadaEm = Instant.now();
        }
    }
}
