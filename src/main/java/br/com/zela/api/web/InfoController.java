package br.com.zela.api.web;

import br.com.zela.api.config.ZelaProperties;
import br.com.zela.api.web.dto.InfoResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Informações públicas da API: nome, versão e ambiente. Útil para conferir qual versão está no ar. */
@Tag(name = "Info", description = "Versão e ambiente da API")
@SecurityRequirements
@RestController
@RequestMapping("/info")
public class InfoController {

    private final ZelaProperties propriedades;
    private final ObjectProvider<BuildProperties> build;

    public InfoController(ZelaProperties propriedades, ObjectProvider<BuildProperties> build) {
        this.propriedades = propriedades;
        this.build = build;
    }

    @Operation(summary = "Versão e ambiente da API")
    @GetMapping
    public InfoResposta info() {
        BuildProperties b = build.getIfAvailable();
        return new InfoResposta(
                "Zela API",
                b != null ? b.getVersion() : "desconhecida",
                propriedades.ambiente(),
                propriedades.mensagem(),
                Instant.now());
    }
}
