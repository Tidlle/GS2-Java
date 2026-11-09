package br.com.zela.api.dominio;

/** Papéis de acesso. Cada papel vê e faz coisas diferentes na API. */
public enum Papel {
    /** Morador: reporta, confirma problemas dos vizinhos e avalia a solução. */
    CIDADAO,
    /** Subprefeitura ou secretaria: trata exceções, emite e fiscaliza as OS. */
    GESTOR,
    /** Equipe própria ou empresa contratada: aceita, executa e conclui as OS. */
    EXECUTOR,
    /** Coordenação: recebe escalonamentos e acompanha indicadores. */
    SUPERVISOR
}
