package br.com.zela.api.ia;

/**
 * Resultado da comparação entre a foto do antes e a do depois.
 *
 * @param resolvido  true se a foto do depois mostra o problema resolvido no mesmo local
 * @param confianca  0 a 1 (alimenta o aceite tácito: alta confiança e sem contestação em 48 h encerra a OS)
 * @param observacao explicação curta, em português
 */
public record Comparacao(boolean resolvido, double confianca, String observacao) {
}
