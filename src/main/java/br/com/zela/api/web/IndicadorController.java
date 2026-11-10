package br.com.zela.api.web;

import br.com.zela.api.servico.IndicadorService;
import br.com.zela.api.web.dto.BairroIndicador;
import br.com.zela.api.web.dto.ExecutorIndicador;
import br.com.zela.api.web.dto.IndicadoresResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Indicadores", description = "Fiscalização da execução e dos contratos")
@RestController
@RequestMapping("/indicadores")
@PreAuthorize("hasAnyRole('GESTOR', 'SUPERVISOR')")
public class IndicadorController {

    private final IndicadorService indicadores;

    public IndicadorController(IndicadorService indicadores) {
        this.indicadores = indicadores;
    }

    @Operation(summary = "Painel geral", description = "OS abertas, atrasadas, prazos cumpridos, retrabalho, exceções e bairros.")
    @GetMapping
    public IndicadoresResposta painel() {
        return indicadores.painel();
    }

    @Operation(summary = "Desempenho por executor", description = "Tempo médio, prazos cumpridos, retrabalho e avaliação dos cidadãos.")
    @GetMapping("/executores")
    public List<ExecutorIndicador> executores() {
        return indicadores.executores();
    }

    @Operation(summary = "Tempo de resolução por bairro", description = "Do mais rápido para o mais lento.")
    @GetMapping("/bairros")
    public List<BairroIndicador> bairros() {
        return indicadores.bairros();
    }
}
