package br.com.zela.api.ia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Escolhe a IA ativa. Com IA_PROVEDOR=google-genai e GEMINI_API_KEY, o Spring AI cria o modelo do Gemini
 * e usamos {@link GeminiServicoIA}; sem isso, {@link IaSimulada}. Assim a API sempre sobe.
 */
@Configuration
public class IaConfig {

    private static final Logger log = LoggerFactory.getLogger(IaConfig.class);

    @Bean
    ServicoIA servicoIA(ObjectProvider<ChatModel> modelo) {
        ChatModel chatModel = modelo.getIfAvailable();
        if (chatModel != null) {
            log.info("IA: Google Gemini via Spring AI ({})", chatModel.getClass().getSimpleName());
            return new GeminiServicoIA(ChatClient.builder(chatModel));
        }
        log.info("IA: simulada (defina IA_PROVEDOR=google-genai e GEMINI_API_KEY para usar o Gemini)");
        return new IaSimulada();
    }
}
