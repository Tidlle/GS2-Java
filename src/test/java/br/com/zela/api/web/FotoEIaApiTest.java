package br.com.zela.api.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** Upload de fotos, triagem e comparação antes/depois com a IA simulada (a mesma interface do Gemini). */
class FotoEIaApiTest extends TesteDeApi {

    /** Cabeçalho PNG válido seguido de bytes variáveis (o servidor confere a assinatura do arquivo). */
    static byte[] png(int semente) {
        byte[] b = new byte[64];
        byte[] assinatura = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(assinatura, 0, b, 0, assinatura.length);
        Arrays.fill(b, 8, b.length, (byte) semente);
        return b;
    }

    static MockMultipartFile arquivo(byte[] dados) {
        return new MockMultipartFile("arquivo", "foto.png", "image/png", dados);
    }

    private String enviarFoto(String email, byte[] dados) throws Exception {
        String r = mvc.perform(como(email, multipart("/fotos").file(arquivo(dados))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(r, "$.url");
    }

    // ---------- Fotos ----------

    @Test
    void uploadDevolveUrlPublica() throws Exception {
        String url = enviarFoto(CIDADAO, png(1));
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(png(1)));
    }

    @Test
    void recusaArquivoQueNaoEhImagem() throws Exception {
        MockMultipartFile falso = new MockMultipartFile("arquivo", "foto.png", "image/png", "<script>".getBytes());
        mvc.perform(como(CIDADAO, multipart("/fotos").file(falso)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("A foto deve ser uma imagem JPEG, PNG ou WEBP"));
    }

    @Test
    void uploadExigeLoginEPapel() throws Exception {
        mvc.perform(multipart("/fotos").file(arquivo(png(1)))).andExpect(status().isUnauthorized());
        mvc.perform(como(GESTOR, multipart("/fotos").file(arquivo(png(1))))).andExpect(status().isForbidden());
    }

    @Test
    void nomeInvalidoOuInexistenteDa404() throws Exception {
        mvc.perform(get("/fotos/application.yml")).andExpect(status().isNotFound());
        mvc.perform(get("/fotos/00000000000000000000000000000000.png")).andExpect(status().isNotFound());
    }

    // ---------- Triagem ----------

    @Test
    void triagemSalvaAFotoESugereCategoria() throws Exception {
        mvc.perform(como(CIDADAO, multipart("/ocorrencias/triagem").file(arquivo(png(7)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl", startsWith("/fotos/")))
                .andExpect(jsonPath("$.categoriaId", notNullValue()))
                .andExpect(jsonPath("$.categoriaNome", notNullValue()))
                .andExpect(jsonPath("$.descricao", notNullValue()))
                .andExpect(jsonPath("$.confianca").value(0.75))
                .andExpect(jsonPath("$.origem").value("simulada"))
                .andExpect(jsonPath("$.urgente").value(false));
    }

    @Test
    void triagemEhSoDoCidadao() throws Exception {
        mvc.perform(como(EXECUTOR, multipart("/ocorrencias/triagem").file(arquivo(png(7)))))
                .andExpect(status().isForbidden());
    }

    // ---------- Comparação antes/depois na conclusão ----------

    /** Cidadão reporta com foto (urgente, iluminação → OS imediata para a Equipe Luz Norte); devolve o id da OS. */
    private Integer osComFotoDoAntes(byte[] antes) throws Exception {
        String fotoAntes = enviarFoto(CIDADAO, antes);
        String r = mvc.perform(json(como(CIDADAO, post("/ocorrencias")), """
                        {"categoriaId": 1, "descricao": "Fio solto no poste.", "latitude": -23.5891,
                         "longitude": -46.6348, "fotoAntesUrl": "%s", "urgente": true}""".formatted(fotoAntes)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer osId = JsonPath.read(r, "$.osId");
        mvc.perform(como(EXECUTOR, post("/ordens/" + osId + "/aceitar"))).andExpect(status().isOk());
        mvc.perform(json(como(EXECUTOR, post("/ordens/" + osId + "/checkin")),
                        "{\"latitude\": -23.5891, \"longitude\": -46.6348}"))
                .andExpect(status().isOk());
        return osId;
    }

    @Test
    void conclusaoGravaAConfiancaDaIa() throws Exception {
        Integer osId = osComFotoDoAntes(png(10));
        String fotoDepois = enviarFoto(EXECUTOR, png(20));

        mvc.perform(json(como(EXECUTOR, post("/ordens/" + osId + "/concluir")),
                        "{\"fotoDepoisUrl\": \"%s\"}".formatted(fotoDepois)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.confiancaIA").value(0.85))
                .andExpect(jsonPath("$.fotoDepoisUrl").value(fotoDepois))
                .andExpect(jsonPath("$.timeline[*].usuario", hasItem("IA (simulada)")))
                .andExpect(jsonPath("$.timeline[-1:].justificativa", hasItem(startsWith("Resolvido com 85% de confiança"))));
    }

    @Test
    void mesmaFotoNoAntesENoDepoisTemConfiancaBaixa() throws Exception {
        Integer osId = osComFotoDoAntes(png(30));
        String mesma = enviarFoto(EXECUTOR, png(30));
        mvc.perform(json(como(EXECUTOR, post("/ordens/" + osId + "/concluir")),
                        "{\"fotoDepoisUrl\": \"%s\"}".formatted(mesma)))
                .andExpect(jsonPath("$.confiancaIA").value(0.0))
                .andExpect(jsonPath("$.timeline[-1:].justificativa", hasItem(startsWith("Não resolvido"))));
    }
}
