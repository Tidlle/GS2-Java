package br.com.zela.api.web;

import br.com.zela.api.armazenamento.ArmazenamentoFotos;
import br.com.zela.api.armazenamento.Imagem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Fotos", description = "Upload das fotos do antes (cidadão) e do depois (executor)")
@RestController
@RequestMapping("/fotos")
public class FotoController {

    private final ArmazenamentoFotos fotos;

    public FotoController(ArmazenamentoFotos fotos) {
        this.fotos = fotos;
    }

    @Operation(summary = "Envia uma foto", description = "Multipart com o campo 'arquivo' (JPEG, PNG ou WEBP, até 8 MB). Devolve a URL.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CIDADAO', 'EXECUTOR')")
    public ResponseEntity<Map<String, String>> enviar(@RequestPart("arquivo") MultipartFile arquivo) {
        String url = fotos.salvar(arquivo);
        return ResponseEntity.created(URI.create(url)).body(Map.of("url", url));
    }

    @Operation(summary = "Baixa uma foto", description = "Pública: o nome é aleatório e só existe para quem recebeu a URL.")
    @SecurityRequirements
    @GetMapping("/{nome}")
    public ResponseEntity<byte[]> baixar(@PathVariable String nome) {
        Imagem imagem = fotos.carregar(nome);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.tipo().mime))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(imagem.dados());
    }
}
