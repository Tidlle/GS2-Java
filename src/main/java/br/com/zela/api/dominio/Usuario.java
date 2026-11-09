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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Pessoa que usa o Zela. O papel define o que ela pode fazer; a senha é guardada só como hash BCrypt. */
@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    public static final int REPUTACAO_INICIAL = 50;
    public static final int REPUTACAO_MAXIMA = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @Column(length = 80)
    private String bairro;

    /** Reputação do cidadão (0 a 100): sobe quando os vizinhos confirmam o que ele reportou. */
    @Column(nullable = false)
    private int reputacao;

    /** Executor ao qual o usuário pertence (só para o papel EXECUTOR). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executor_id")
    private Executor executor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    private Usuario(String nome, String email, String senhaHash, Papel papel, String bairro, Executor executor) {
        this.nome = nome;
        this.email = email.trim().toLowerCase();
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.bairro = bairro;
        this.executor = executor;
        this.reputacao = REPUTACAO_INICIAL;
    }

    /** Cadastro público: todo novo usuário é cidadão. */
    public static Usuario novoCidadao(String nome, String email, String senhaHash, String bairro) {
        return new Usuario(nome, email, senhaHash, Papel.CIDADAO, bairro, null);
    }

    public static Usuario novoMembroDeExecutor(String nome, String email, String senhaHash, Executor executor) {
        return new Usuario(nome, email, senhaHash, Papel.EXECUTOR, null, executor);
    }

    public void ajustarReputacao(int delta) {
        reputacao = Math.clamp(reputacao + delta, 0, REPUTACAO_MAXIMA);
    }

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }
}
