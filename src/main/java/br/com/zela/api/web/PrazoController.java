package br.com.zela.api.web;

import br.com.zela.api.agendamento.CicloDePrazos;
import br.com.zela.api.agendamento.ResumoCiclo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Prazos", description = "Confirmação em 24 h, escalonamento e aceite tácito (rodam sozinhos a cada 5 min)")
@RestController
@RequestMapping("/prazos")
public class PrazoController {

    private final CicloDePrazos ciclo;

    public PrazoController(CicloDePrazos ciclo) {
        this.ciclo = ciclo;
    }

    @Operation(summary = "Executa agora a verificação de prazos",
            description = "Mesmo ciclo do agendador, disparado na hora (útil na demonstração).")
    @PostMapping("/executar")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'GESTOR')")
    public ResumoCiclo executar() {
        return ciclo.executar();
    }
}
