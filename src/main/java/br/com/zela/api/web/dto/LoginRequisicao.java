package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequisicao(
        @Schema(example = "cidadao@zela.com") @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email,
        @Schema(example = "123456") @NotBlank(message = "Informe a senha") String senha) {
}
