package br.com.zela.api.ia;

import br.com.zela.api.armazenamento.Imagem;
import java.util.List;

/**
 * IA generativa do Zela (Spring AI). Duas implementações: Gemini (real) e simulada (sem chave, usada em
 * desenvolvimento, testes e CI). Quem chama não sabe qual está ativa.
 */
public interface ServicoIA {

    /** Classifica a foto do cidadão entre as categorias informadas e descreve o problema. */
    Triagem triar(Imagem foto, List<String> categorias);

    /** Compara o antes e o depois e diz se o problema foi resolvido. */
    Comparacao comparar(Imagem antes, Imagem depois, String descricaoDoProblema);

    /** "gemini" ou "simulada", exibido nas respostas para transparência. */
    String origem();
}
