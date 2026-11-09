package br.com.zela.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RotasPublicasTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void infoEhPublicaETrazVersaoEAmbiente() throws Exception {
        mvc.perform(get("/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Zela API"))
                .andExpect(jsonPath("$.ambiente").value("test"))
                .andExpect(jsonPath("$.versao").isNotEmpty());
    }

    @Test
    void healthCheckEhPublicoEEstaNoAr() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void documentacaoOpenApiEhPublica() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Zela API"));
    }

    @Test
    void rotaNaoPublicaExigeAutenticacao() throws Exception {
        mvc.perform(get("/categorias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerDocumentaOEsquemaBearer() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").isEmpty());
    }
}
