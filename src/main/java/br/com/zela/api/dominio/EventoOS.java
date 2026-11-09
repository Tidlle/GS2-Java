package br.com.zela.api.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Item da timeline e da auditoria da OS: quem fez o quê, quando e por quê.
 * Sem mudança de status (ex.: prazo alterado, cobrança do supervisor), statusAnterior fica vazio.
 */
@Entity
@Table(name = "eventos_os")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_id", nullable = false)
    private OrdemServico ordem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusOS statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusOS statusNovo;

    /** Usuário que agiu; vazio quando foi o sistema (emissão automática, agendadores). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Nome exibido na timeline (ex.: "Carlos Lima", "Sistema (emissão automática)"). */
    @Column(name = "autor_nome", nullable = false, length = 120)
    private String autorNome;

    @Column(length = 500)
    private String justificativa;

    @Column(nullable = false)
    private Instant data;

    EventoOS(OrdemServico ordem, StatusOS statusAnterior, StatusOS statusNovo, Ator ator, String justificativa) {
        this.ordem = ordem;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.usuario = ator.usuario();
        this.autorNome = ator.nome();
        this.justificativa = justificativa;
        this.data = Instant.now();
    }
}
