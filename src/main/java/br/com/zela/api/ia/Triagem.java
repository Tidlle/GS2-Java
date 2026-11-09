package br.com.zela.api.ia;

/**
 * Resultado da triagem da foto.
 *
 * @param categoria  nome de uma das categorias cadastradas (ou vazio se a IA não reconheceu)
 * @param descricao  descrição padronizada do problema, em português
 * @param urgente    true se a foto indica risco à vida (fio caído, árvore na via…)
 * @param confianca  0 a 1
 */
public record Triagem(String categoria, String descricao, boolean urgente, double confianca) {
}
