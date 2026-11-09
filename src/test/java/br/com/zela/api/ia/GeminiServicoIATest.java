package br.com.zela.api.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.zela.api.armazenamento.Imagem;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Testa a integração com o Spring AI sem chamar o Gemini: um ChatModel falso devolve o JSON que o modelo
 * devolveria. Assim conferimos o prompt (texto + imagens) e a conversão da resposta em record.
 */
class GeminiServicoIATest {

    private final Imagem foto = new Imagem(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2}, Imagem.Tipo.JPEG);
    private final AtomicReference<Prompt> recebido = new AtomicReference<>();

    private GeminiServicoIA ia(String respostaDoModelo) {
        ChatModel falso = prompt -> {
            recebido.set(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(respostaDoModelo))));
        };
        return new GeminiServicoIA(ChatClient.builder(falso));
    }

    @Test
    void triagemEnviaAFotoEConverteAResposta() {
        Triagem t = ia("""
                {"categoria": "Buraco", "descricao": "Buraco fundo no asfalto.", "urgente": false, "confianca": 0.91}""")
                .triar(foto, List.of("Iluminação", "Buraco"));

        assertThat(t).isEqualTo(new Triagem("Buraco", "Buraco fundo no asfalto.", false, 0.91));
        var mensagem = recebido.get().getUserMessage();
        assertThat(mensagem.getMedia()).hasSize(1);
        assertThat(mensagem.getMedia().getFirst().getMimeType().toString()).isEqualTo("image/jpeg");
        assertThat(mensagem.getText()).contains("Iluminação, Buraco");
        assertThat(recebido.get().getSystemMessage().getText()).contains("Zela");
    }

    @Test
    void comparacaoEnviaAsDuasFotos() {
        Comparacao c = ia("""
                {"resolvido": true, "confianca": 0.88, "observacao": "Lâmpada acesa no mesmo poste."}""")
                .comparar(foto, foto, "Poste apagado");

        assertThat(c.resolvido()).isTrue();
        assertThat(c.confianca()).isEqualTo(0.88);
        assertThat(recebido.get().getUserMessage().getMedia()).hasSize(2);
        assertThat(recebido.get().getUserMessage().getText()).contains("Poste apagado");
    }

    @Test
    void respostaInvalidaViraFalhaIA() {
        assertThatThrownBy(() -> ia("não é JSON").triar(foto, List.of("Buraco")))
                .isInstanceOf(FalhaIAException.class);
    }
}
