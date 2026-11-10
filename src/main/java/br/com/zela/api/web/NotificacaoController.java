package br.com.zela.api.web;

import br.com.zela.api.servico.NotificacaoService;
import br.com.zela.api.web.dto.NotificacaoResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notificações", description = "Avisos gerados a partir dos eventos publicados no RabbitMQ")
@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacoes;

    public NotificacaoController(NotificacaoService notificacoes) {
        this.notificacoes = notificacoes;
    }

    @Operation(summary = "Minhas notificações", description = "Mais recentes primeiro.")
    @GetMapping
    public Page<NotificacaoResposta> minhas(
            @ParameterObject @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC)
            Pageable paginacao) {
        return notificacoes.minhas(paginacao);
    }

    @Operation(summary = "Quantas não lidas", description = "Para o contador do sino no app.")
    @GetMapping("/nao-lidas")
    public Map<String, Long> naoLidas() {
        return Map.of("naoLidas", notificacoes.naoLidas());
    }

    @Operation(summary = "Marca todas como lidas")
    @PostMapping("/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas() {
        notificacoes.marcarTodasComoLidas();
        return ResponseEntity.noContent().build();
    }
}
