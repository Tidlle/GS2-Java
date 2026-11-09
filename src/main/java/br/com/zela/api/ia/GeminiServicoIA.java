package br.com.zela.api.ia;

import br.com.zela.api.armazenamento.Imagem;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;

/**
 * IA real: Google Gemini via Spring AI (ChatClient multimodal + saída estruturada em record).
 * A imagem vai junto com o texto; o Spring AI acrescenta ao prompt o formato JSON esperado
 * e converte a resposta em {@link Triagem} ou {@link Comparacao}.
 */
public class GeminiServicoIA implements ServicoIA {

    private static final String SISTEMA = """
            Você é o assistente de triagem do Zela, um app de zeladoria urbana de uma prefeitura brasileira.
            Analise fotos de problemas em espaços públicos (buracos, postes apagados, entulho, calçadas, árvores).
            Responda sempre em português do Brasil, de forma objetiva e sem inventar detalhes que não aparecem na foto.
            """;

    private final ChatClient chat;

    public GeminiServicoIA(ChatClient.Builder builder) {
        this.chat = builder.defaultSystem(SISTEMA).build();
    }

    @Override
    public Triagem triar(Imagem foto, List<String> categorias) {
        String pedido = """
                Classifique o problema da foto em UMA destas categorias: %s.
                - "categoria": exatamente um dos nomes da lista; use "" se a foto não mostrar nenhum desses problemas.
                - "descricao": uma frase (até 200 caracteres) descrevendo o problema e o risco para quem passa.
                - "urgente": true só se houver risco imediato à vida (fio elétrico caído, árvore sobre a via,
                  buraco que engole um carro, desabamento); caso contrário false.
                - "confianca": número de 0 a 1 indicando sua certeza na categoria.
                """.formatted(String.join(", ", categorias));
        try {
            return chat.prompt()
                    .user(u -> u.text(pedido).media(MimeType.valueOf(foto.tipo().mime), new ByteArrayResource(foto.dados())))
                    .call()
                    .entity(Triagem.class);
        } catch (RuntimeException e) {
            throw new FalhaIAException("Falha na triagem pelo Gemini", e);
        }
    }

    @Override
    public Comparacao comparar(Imagem antes, Imagem depois, String descricaoDoProblema) {
        String pedido = """
                A primeira imagem é o ANTES e a segunda é o DEPOIS de um serviço de zeladoria.
                Problema reportado: "%s".
                - "resolvido": true se o DEPOIS mostra o mesmo local com o problema resolvido.
                - "confianca": número de 0 a 1. Use valores baixos se as fotos parecerem de locais diferentes,
                  se o DEPOIS estiver escuro/borrado ou se o reparo parecer parcial.
                - "observacao": uma frase explicando a decisão.
                """.formatted(descricaoDoProblema);
        try {
            return chat.prompt()
                    .user(u -> u.text(pedido)
                            .media(MimeType.valueOf(antes.tipo().mime), new ByteArrayResource(antes.dados()))
                            .media(MimeType.valueOf(depois.tipo().mime), new ByteArrayResource(depois.dados())))
                    .call()
                    .entity(Comparacao.class);
        } catch (RuntimeException e) {
            throw new FalhaIAException("Falha na comparação pelo Gemini", e);
        }
    }

    @Override
    public String origem() {
        return "gemini";
    }
}
