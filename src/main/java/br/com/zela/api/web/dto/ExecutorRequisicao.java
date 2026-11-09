package br.com.zela.api.web.dto;

import br.com.zela.api.dominio.TipoExecutor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Criação e edição de executor (somente GESTOR). */
public record ExecutorRequisicao(
        @Schema(example = "Poda Verde Serviços")
        @NotBlank(message = "Informe o nome") @Size(max = 120, message = "Nome com no máximo 120 caracteres") String nome,

        @Schema(example = "EMPRESA")
        @NotNull(message = "Informe o tipo (EQUIPE_PROPRIA, EMPRESA ou CONCESSIONARIA)") TipoExecutor tipo,

        @Schema(example = "Zona Sul")
        @NotBlank(message = "Informe a região") @Size(max = 80, message = "Região com no máximo 80 caracteres") String regiao,

        @Schema(example = "Contrato 045/2026 — Poda de árvores")
        @NotBlank(message = "Informe o contrato") @Size(max = 160, message = "Contrato com no máximo 160 caracteres") String contrato) {
}
