package br.com.zela.api.ia;

import br.com.zela.api.armazenamento.Imagem;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * IA simulada, determinística e sem rede: usada quando não há chave do Gemini (desenvolvimento, testes, CI).
 * A mesma foto sempre gera a mesma categoria, o que deixa os testes reprodutíveis.
 */
public class IaSimulada implements ServicoIA {

    private static final Map<String, String> DESCRICOES = Map.of(
            "Iluminação", "Poste de iluminação pública apagado, deixando o trecho escuro à noite.",
            "Buraco", "Buraco no asfalto com risco para veículos e motos.",
            "Entulho", "Entulho acumulado na calçada, bloqueando a passagem de pedestres.",
            "Calçada", "Calçada quebrada, com risco de queda para pedestres.",
            "Árvore", "Árvore com galhos comprometidos próxima à via.");

    @Override
    public Triagem triar(Imagem foto, List<String> categorias) {
        if (categorias.isEmpty()) {
            return new Triagem("", "Não foi possível identificar o problema.", false, 0);
        }
        String categoria = categorias.get(Math.floorMod(Arrays.hashCode(foto.dados()), categorias.size()));
        String descricao = DESCRICOES.getOrDefault(categoria, "Problema de " + categoria.toLowerCase() + " na via pública.");
        return new Triagem(categoria, descricao, false, 0.75);
    }

    @Override
    public Comparacao comparar(Imagem antes, Imagem depois, String descricaoDoProblema) {
        if (Arrays.equals(antes.dados(), depois.dados())) {
            return new Comparacao(false, 0.1, "A foto do depois é igual à do antes.");
        }
        return new Comparacao(true, 0.85, "Simulação: o depois difere do antes; considerado resolvido.");
    }

    @Override
    public String origem() {
        return "simulada";
    }
}
