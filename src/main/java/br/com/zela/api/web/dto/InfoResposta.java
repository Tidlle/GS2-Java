package br.com.zela.api.web.dto;

import java.time.Instant;

/** Resposta de GET /info. */
public record InfoResposta(String nome, String versao, String ambiente, String mensagem, Instant dataHora) {
}
