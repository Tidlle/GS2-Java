package br.com.zela.api.web.dto;

/** Transparência por bairro: quantas ocorrências, quantas resolvidas e o tempo médio até a solução. */
public record BairroIndicador(String bairro, long ocorrencias, long resolvidas, Double horas) {
}
