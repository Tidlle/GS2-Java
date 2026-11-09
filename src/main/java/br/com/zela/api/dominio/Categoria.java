package br.com.zela.api.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tipo de problema (iluminação, buraco, entulho…), ligado ao executor responsável pelo contrato
 * e ao prazo contratual. Define se a OS é emitida automaticamente ou vai para a fila de exceções.
 */
@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nome;

    /** Nome do ícone usado no app (Ionicons). */
    @Column(nullable = false, length = 40)
    private String icone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "executor_responsavel_id", nullable = false)
    private Executor executorResponsavel;

    /** Prazo contratual, em horas, para resolver a OS desta categoria. */
    @Column(name = "prazo_horas", nullable = false)
    private int prazoHoras;

    /** true: OS emitida direto ao executor; false: vai para a fila de exceções do gestor. */
    @Column(name = "emissao_automatica", nullable = false)
    private boolean emissaoAutomatica;

    /** Risco base de 1 (baixo) a 5 (risco à vida); é a prioridade inicial da ocorrência. */
    @Column(name = "risco_base", nullable = false)
    private int riscoBase;

    public Categoria(String nome, String icone, Executor executorResponsavel, int prazoHoras,
            boolean emissaoAutomatica, int riscoBase) {
        this.nome = nome;
        this.icone = icone;
        this.executorResponsavel = executorResponsavel;
        this.prazoHoras = prazoHoras;
        this.emissaoAutomatica = emissaoAutomatica;
        this.riscoBase = riscoBase;
    }
}
