package br.com.zela.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ZelaApiApplicationTests {

    @Test
    void contextoSobeSemServicosExternos() {
        // Garante que a API sobe sem RabbitMQ e sem chave de IA, como no pipeline de CI.
    }
}
