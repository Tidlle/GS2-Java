package br.com.zela.api.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class AutenticacaoTest extends TesteDeApi {

    @Test
    void bloqueiaDepoisDeCincoSenhasErradas() throws Exception {
        mvc.perform(json(post("/auth/cadastro"), """
                        {"nome": "Teste Força Bruta", "email": "forca@bruta.com", "senha": "certa123", "bairro": "Saúde"}"""))
                .andExpect(status().isCreated());
        for (int i = 0; i < 5; i++) {
            mvc.perform(json(post("/auth/login"), """
                            {"email": "forca@bruta.com", "senha": "errada"}"""))
                    .andExpect(status().isUnauthorized());
        }
        // Mesmo com a senha certa, o e-mail fica bloqueado durante a janela de 15 minutos.
        mvc.perform(json(post("/auth/login"), """
                        {"email": "FORCA@bruta.com", "senha": "certa123"}"""))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Muitas tentativas"));
    }

    @Test
    void tokenDeOutroEmissorDa401() throws Exception {
        // Assinado com o mesmo segredo, mas com outro "iss": precisa ser recusado.
        var chave = new SecretKeySpec(
                "teste-zela-segredo-com-pelo-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var claims = JwtClaimsSet.builder()
                .issuer("outro-sistema").subject("2").claim("papel", "GESTOR")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        String token = new NimbusJwtEncoder(new ImmutableSecret<>(chave))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        mvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginDevolveTokenEUsuarioSemSenha() throws Exception {
        mvc.perform(json(post("/auth/login"), """
                        {"email": "GESTOR@zela.com", "senha": "123456"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.usuario.papel").value("GESTOR"))
                .andExpect(jsonPath("$.usuario.nome").value("Carlos Lima"))
                .andExpect(jsonPath("$.usuario.senha").doesNotExist())
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());
    }

    @Test
    void loginDoExecutorTrazOExecutorVinculado() throws Exception {
        mvc.perform(json(post("/auth/login"), """
                        {"email": "executor@zela.com", "senha": "123456"}"""))
                .andExpect(jsonPath("$.usuario.executorId").value(1))
                .andExpect(jsonPath("$.usuario.tipo").value("EMPRESA"));
    }

    @Test
    void senhaErradaDa401ComMensagemGenerica() throws Exception {
        mvc.perform(json(post("/auth/login"), """
                        {"email": "gestor@zela.com", "senha": "errada"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));

        mvc.perform(json(post("/auth/login"), """
                        {"email": "ninguem@zela.com", "senha": "123456"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
    }

    @Test
    void loginSemCamposDa400ComListaDeErros() throws Exception {
        mvc.perform(json(post("/auth/login"), """
                        {"email": "nao-e-email", "senha": ""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros", hasSize(2)))
                .andExpect(jsonPath("$.erros[0].campo").value("email"))
                .andExpect(jsonPath("$.erros[1].campo").value("senha"));
    }

    @Test
    void jsonMalformadoDa400Padronizado() throws Exception {
        mvc.perform(json(post("/auth/login"), "{ isso não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void cadastroCriaCidadaoQueConsegueLogar() throws Exception {
        mvc.perform(json(post("/auth/cadastro"), """
                        {"nome": "Maria Oliveira", "email": "Maria@Email.com", "senha": "segredo1", "bairro": "Saúde"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/usuarios/me"))
                .andExpect(jsonPath("$.papel").value("CIDADAO"))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.reputacao").value(50));

        mvc.perform(json(post("/auth/login"), """
                        {"email": "maria@email.com", "senha": "segredo1"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void cadastroComEmailRepetidoDa409() throws Exception {
        mvc.perform(json(post("/auth/cadastro"), """
                        {"nome": "Outra Ana", "email": "cidadao@zela.com", "senha": "segredo1", "bairro": "Saúde"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma conta com esse e-mail"));
    }

    @Test
    void cadastroValidaSenhaCurtaENomeVazio() throws Exception {
        mvc.perform(json(post("/auth/cadastro"), """
                        {"nome": "", "email": "x@x.com", "senha": "123", "bairro": "Saúde"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'senha')].mensagem")
                        .value("A senha deve ter entre 6 e 72 caracteres"))
                .andExpect(jsonPath("$.erros[?(@.campo == 'nome')]").exists());
    }

    @Test
    void euComTokenDevolveOUsuarioLogado() throws Exception {
        mvc.perform(como(CIDADAO, get("/usuarios/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(CIDADAO))
                .andExpect(jsonPath("$.reputacao").value(82));
    }

    @Test
    void semTokenDa401() throws Exception {
        mvc.perform(get("/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://zela.app/erros/nao-autenticado"));
    }

    @Test
    void tokenInvalidoDa401() throws Exception {
        mvc.perform(get("/usuarios/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAdulteradoDa401() throws Exception {
        String token = token(CIDADAO);
        String adulterado = token.substring(0, token.length() - 3) + "abc";
        mvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }
}
