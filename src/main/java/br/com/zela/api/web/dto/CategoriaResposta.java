package br.com.zela.api.web.dto;

import br.com.zela.api.dominio.Categoria;

/** Mesmos campos do tipo Categoria do app, mais o nome do executor responsável. */
public record CategoriaResposta(
        Long id,
        String nome,
        String icone,
        Long executorResponsavelId,
        String executorResponsavelNome,
        int prazoHoras,
        boolean emissaoAutomatica,
        int riscoBase) {

    public static CategoriaResposta de(Categoria c) {
        return new CategoriaResposta(c.getId(), c.getNome(), c.getIcone(), c.getExecutorResponsavel().getId(),
                c.getExecutorResponsavel().getNome(), c.getPrazoHoras(), c.isEmissaoAutomatica(), c.getRiscoBase());
    }
}
