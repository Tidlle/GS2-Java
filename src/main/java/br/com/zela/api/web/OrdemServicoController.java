package br.com.zela.api.web;

import br.com.zela.api.dominio.StatusOS;
import br.com.zela.api.servico.OrdemServicoService;
import br.com.zela.api.web.dto.AvaliacaoRequisicao;
import br.com.zela.api.web.dto.CheckinRequisicao;
import br.com.zela.api.web.dto.ConclusaoRequisicao;
import br.com.zela.api.web.dto.EncaminhamentoRequisicao;
import br.com.zela.api.web.dto.JustificativaRequisicao;
import br.com.zela.api.web.dto.OrdemResposta;
import br.com.zela.api.web.dto.PrazoRequisicao;
import br.com.zela.api.web.dto.ReatribuicaoRequisicao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ordens de serviço", description = "Ciclo da OS: executor executa, gestor fiscaliza, cidadão avalia, supervisor cobra")
@RestController
@RequestMapping("/ordens")
public class OrdemServicoController {

    private final OrdemServicoService ordens;

    public OrdemServicoController(OrdemServicoService ordens) {
        this.ordens = ordens;
    }

    // ---------- Consultas ----------

    @Operation(summary = "Painel de OS (paginado)", description = "Filtros combináveis: status, executor, bairro e atrasadas.")
    @GetMapping
    @PreAuthorize("hasAnyRole('GESTOR', 'SUPERVISOR')")
    public Page<OrdemResposta> listar(
            @RequestParam(required = false) StatusOS status,
            @RequestParam(required = false) Long executorId,
            @Parameter(description = "Bairro da ocorrência (contém)") @RequestParam(required = false) String bairro,
            @Parameter(description = "Só etapas abertas com prazo vencido") @RequestParam(required = false) Boolean atrasadas,
            @ParameterObject @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC)
            Pageable paginacao) {
        return ordens.listar(status, executorId, bairro, atrasadas, paginacao);
    }

    @Operation(summary = "OS da minha equipe", description = "Para o executor logado; filtro opcional por status.")
    @GetMapping("/minhas")
    @PreAuthorize("hasRole('EXECUTOR')")
    public Page<OrdemResposta> minhas(@RequestParam(required = false) StatusOS status,
            @ParameterObject @PageableDefault(size = 20, sort = "prazoEtapa", direction = Sort.Direction.ASC)
            Pageable paginacao) {
        return ordens.minhas(status, paginacao);
    }

    @Operation(summary = "Escalonamentos", description = "OS escaladas ou com prazo vencido, as mais atrasadas primeiro.")
    @GetMapping("/escalonadas")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'GESTOR')")
    public List<OrdemResposta> escalonadas() {
        return ordens.escalonadas();
    }

    @Operation(summary = "Detalhe com timeline",
            description = "Gestor e supervisor: qualquer OS. Executor: só da equipe. Cidadão: só das próprias ocorrências.")
    @GetMapping("/{id}")
    public OrdemResposta buscar(@PathVariable Long id) {
        return ordens.buscar(id);
    }

    // ---------- Executor ----------

    @Operation(summary = "Aceita a OS")
    @PostMapping("/{id}/aceitar")
    @PreAuthorize("hasRole('EXECUTOR')")
    public OrdemResposta aceitar(@PathVariable Long id) {
        return ordens.aceitar(id);
    }

    @Operation(summary = "Recusa a OS", description = "Motivo obrigatório (ex.: fora da competência). Volta ao gestor.")
    @PostMapping("/{id}/recusar")
    @PreAuthorize("hasRole('EXECUTOR')")
    public OrdemResposta recusar(@PathVariable Long id, @Valid @RequestBody JustificativaRequisicao req) {
        return ordens.recusar(id, req.justificativa());
    }

    @Operation(summary = "Check-in no local", description = "Libera a execução só a até 100 m do local (senão 422 com a distância).")
    @PostMapping("/{id}/checkin")
    @PreAuthorize("hasRole('EXECUTOR')")
    public OrdemResposta checkin(@PathVariable Long id, @Valid @RequestBody CheckinRequisicao req) {
        return ordens.checkin(id, req.latitude(), req.longitude());
    }

    @Operation(summary = "Conclui a OS", description = "Foto do depois obrigatória.")
    @PostMapping("/{id}/concluir")
    @PreAuthorize("hasRole('EXECUTOR')")
    public OrdemResposta concluir(@PathVariable Long id, @Valid @RequestBody ConclusaoRequisicao req) {
        return ordens.concluir(id, req);
    }

    // ---------- Gestor ----------

    @Operation(summary = "Aprova a execução", description = "Encerra a OS e resolve a ocorrência.")
    @PostMapping("/{id}/aprovar")
    @PreAuthorize("hasRole('GESTOR')")
    public OrdemResposta aprovar(@PathVariable Long id) {
        return ordens.aprovar(id);
    }

    @Operation(summary = "Reprova a execução", description = "Motivo obrigatório. A OS volta ao executor como retrabalho (24 h).")
    @PostMapping("/{id}/reprovar")
    @PreAuthorize("hasRole('GESTOR')")
    public OrdemResposta reprovar(@PathVariable Long id, @Valid @RequestBody JustificativaRequisicao req) {
        return ordens.reprovar(id, req.justificativa());
    }

    @Operation(summary = "Altera o prazo da etapa", description = "Novo prazo em horas a partir de agora.")
    @PatchMapping("/{id}/prazo")
    @PreAuthorize("hasRole('GESTOR')")
    public OrdemResposta alterarPrazo(@PathVariable Long id, @Valid @RequestBody PrazoRequisicao req) {
        return ordens.alterarPrazo(id, req.horas());
    }

    @Operation(summary = "Cancela a OS", description = "Motivo obrigatório. A ocorrência volta à fila de exceções.")
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('GESTOR')")
    public OrdemResposta cancelar(@PathVariable Long id, @Valid @RequestBody JustificativaRequisicao req) {
        return ordens.cancelar(id, req.justificativa());
    }

    @Operation(summary = "Encaminha a outro órgão", description = "Usado quando o executor recusa por estar fora da competência.")
    @PostMapping("/{id}/encaminhar")
    @PreAuthorize("hasRole('GESTOR')")
    public OrdemResposta encaminhar(@PathVariable Long id, @Valid @RequestBody EncaminhamentoRequisicao req) {
        return ordens.encaminhar(id, req);
    }

    // ---------- Cidadão ----------

    @Operation(summary = "Confirma ou contesta a solução", description = "Só quem reportou, com a OS concluída.")
    @PostMapping("/{id}/avaliacao")
    @PreAuthorize("hasRole('CIDADAO')")
    public OrdemResposta avaliar(@PathVariable Long id, @Valid @RequestBody AvaliacaoRequisicao req) {
        return ordens.avaliar(id, req);
    }

    // ---------- Supervisor ----------

    @Operation(summary = "Cobra o executor", description = "Registra a cobrança na timeline.")
    @PostMapping("/{id}/cobrar")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public OrdemResposta cobrar(@PathVariable Long id) {
        return ordens.cobrar(id);
    }

    @Operation(summary = "Reatribui a outro executor", description = "A OS volta a EMITIDA com novo prazo.")
    @PostMapping("/{id}/reatribuir")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'GESTOR')")
    public OrdemResposta reatribuir(@PathVariable Long id, @Valid @RequestBody ReatribuicaoRequisicao req) {
        return ordens.reatribuir(id, req.executorId());
    }
}
