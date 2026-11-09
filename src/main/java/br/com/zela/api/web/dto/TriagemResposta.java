package br.com.zela.api.web.dto;

/**
 * Sugestão da IA para o reporte. O app preenche o formulário com ela; o cidadão pode editar antes de enviar.
 *
 * @param fotoUrl     foto já salva (usar em fotoAntesUrl no POST /ocorrencias)
 * @param categoriaId categoria sugerida (vazia se a IA não reconheceu ou falhou)
 * @param origem      "gemini" ou "simulada"
 * @param aviso       mensagem para o cidadão (baixa confiança, urgência com 199/193, IA indisponível)
 */
public record TriagemResposta(
        String fotoUrl,
        Long categoriaId,
        String categoriaNome,
        String descricao,
        boolean urgente,
        Double confianca,
        String origem,
        String aviso) {

    public static TriagemResposta semSugestao(String fotoUrl, String origem, String aviso) {
        return new TriagemResposta(fotoUrl, null, null, "", false, null, origem, aviso);
    }
}
