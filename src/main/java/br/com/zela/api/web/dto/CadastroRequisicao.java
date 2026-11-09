package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cadastro público de cidadão. */
public record CadastroRequisicao(
        @Schema(example = "Maria Oliveira")
        @NotBlank(message = "Informe o nome") @Size(max = 120, message = "Nome com no máximo 120 caracteres") String nome,

        @Schema(example = "maria@email.com")
        @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido")
        @Size(max = 160, message = "E-mail com no máximo 160 caracteres") String email,

        @Schema(example = "segredo123")
        @NotBlank(message = "Informe a senha")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres") String senha,

        @Schema(example = "Vila Mariana")
        @NotBlank(message = "Informe o bairro") @Size(max = 80, message = "Bairro com no máximo 80 caracteres") String bairro) {
}
