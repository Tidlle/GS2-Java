package br.com.zela.api.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Quem executa o serviço: equipe própria, empresa contratada ou concessionária.
 * Os indicadores (tempo médio, prazos cumpridos, retrabalho, avaliação) são calculados a partir das OS.
 */
@Entity
@Table(name = "executores")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Executor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoExecutor tipo;

    @Column(nullable = false, length = 80)
    private String regiao;

    @Column(nullable = false, length = 160)
    private String contrato;

    public Executor(String nome, TipoExecutor tipo, String regiao, String contrato) {
        this.nome = nome;
        this.tipo = tipo;
        this.regiao = regiao;
        this.contrato = contrato;
    }
}
