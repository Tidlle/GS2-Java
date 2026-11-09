package br.com.zela.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Base dos testes de API: sobe a aplicação com H2 e desfaz as alterações de cada teste. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class TesteDeApi {

    static final String CIDADAO = "cidadao@zela.com";
    static final String GESTOR = "gestor@zela.com";
    static final String EXECUTOR = "executor@zela.com";
    static final String SUPERVISOR = "supervisor@zela.com";

    @Autowired
    MockMvc mvc;

    /** Faz login de verdade em POST /auth/login e devolve o token. */
    String token(String email) throws Exception {
        String corpo = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "123456"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    /** Adiciona o cabeçalho Authorization: Bearer com o token do usuário. */
    <B extends AbstractMockHttpServletRequestBuilder<B>> B como(String email, B req) throws Exception {
        return req.header("Authorization", "Bearer " + token(email));
    }

    static <B extends AbstractMockHttpServletRequestBuilder<B>> B json(B req, String corpo) {
        return req.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }
}
