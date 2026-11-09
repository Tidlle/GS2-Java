package br.com.zela.api.web;

import br.com.zela.api.dominio.StatusOcorrencia;
import br.com.zela.api.servico.ConfirmacaoService;
import br.com.zela.api.servico.ExcecaoService;
import br.com.zela.api.servico.OcorrenciaService;
import br.com.zela.api.web.dto.ConfirmacaoRequisicao;
import br.com.zela.api.web.dto.EmissaoOSRequisicao;
import br.com.zela.api.web.dto.EncaminhamentoRequisicao;
import br.com.zela.api.web.dto.JustificativaRequisicao;
import br.com.zela.api.web.dto.OcorrenciaEdicao;
import br.com.zela.api.web.dto.OcorrenciaRequisicao;
import br.com.zela.api.web.dto.OcorrenciaResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ocorrências", description = "Reporte do cidadão, confirmação dos vizinhos e fila de exceções do gestor")
@RestController
@RequestMapping("/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaService ocorrencias;
    private final ConfirmacaoService confirmacoes;
    private final ExcecaoService excecoes;

    public OcorrenciaController(OcorrenciaService ocorrencias, ConfirmacaoService confirmacoes,
            ExcecaoService excecoes) {
        this.ocorrencias = ocorrencias;
        this.confirmacoes = confirmacoes;
        this.excecoes = excecoes;
    }

    // ---------- Cidadão: CRUD ----------

    @Operation(summary = "Reporta um problema",
            description = "Urgente: pula a confirmação e emite a OS do plantão. Normal: aguarda os vizinhos.")
    @PostMapping
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<OcorrenciaResposta> criar(@Valid @RequestBody OcorrenciaRequisicao req) {
        OcorrenciaResposta criada = ocorrencias.criar(req);
        return ResponseEntity.created(URI.create("/ocorrencias/" + criada.id())).body(criada);
    }

    @Operation(summary = "Lista ocorrências",
            description = "Com lat e lng: só as que estão no raio, da mais próxima para a mais distante, com distanciaM.")
    @GetMapping
    public Page<OcorrenciaResposta> listar(
            @Parameter(example = "-23.5893") @RequestParam(required = false) Double lat,
            @Parameter(example = "-46.6347") @RequestParam(required = false) Double lng,
            @Parameter(description = "Raio em metros (padrão 2000)")
            @RequestParam(required = false) @Min(50) @Max(50_000) Integer raio,
            @RequestParam(required = false) StatusOcorrencia status,
            @ParameterObject @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC)
            Pageable paginacao) {
        return ocorrencias.listar(lat, lng, raio, status, paginacao);
    }

    @Operation(summary = "Minhas ocorrências", description = "Reportadas pelo cidadão logado, das mais recentes.")
    @GetMapping("/minhas")
    @PreAuthorize("hasRole('CIDADAO')")
    public Page<OcorrenciaResposta> minhas(
            @ParameterObject @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC)
            Pageable paginacao) {
        return ocorrencias.minhas(paginacao);
    }

    @Operation(summary = "Detalhe de uma ocorrência")
    @GetMapping("/{id}")
    public OcorrenciaResposta buscar(@PathVariable Long id) {
        return ocorrencias.buscar(id);
    }

    @Operation(summary = "Edita a própria ocorrência", description = "Só o autor e só antes de virar OS (senão 403/409).")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CIDADAO')")
    public OcorrenciaResposta editar(@PathVariable Long id, @Valid @RequestBody OcorrenciaEdicao req) {
        return ocorrencias.editar(id, req);
    }

    @Operation(summary = "Exclui a própria ocorrência", description = "Só o autor e só antes de virar OS (senão 403/409).")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        ocorrencias.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Cidadão: confirmação dos vizinhos ----------

    @Operation(summary = "Ocorrências para confirmar",
            description = "De outros cidadãos, aguardando confirmação e ainda não respondidas por você.")
    @GetMapping("/para-confirmar")
    @PreAuthorize("hasRole('CIDADAO')")
    public List<OcorrenciaResposta> paraConfirmar(@RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return confirmacoes.paraConfirmar(lat, lng);
    }

    @Operation(summary = "Responde se o problema ainda existe",
            description = "3 \"sim\": confirmada (rotina vira OS na hora). 3 \"não\": descartada.")
    @PostMapping("/{id}/confirmacoes")
    @PreAuthorize("hasRole('CIDADAO')")
    public OcorrenciaResposta confirmar(@PathVariable Long id, @Valid @RequestBody ConfirmacaoRequisicao req) {
        return confirmacoes.responder(id, req.aindaExiste());
    }

    // ---------- Gestor: fila de exceções ----------

    @Operation(summary = "Fila de exceções", description = "Confirmadas sem OS (sem emissão automática). Maior prioridade primeiro.")
    @GetMapping("/excecoes")
    @PreAuthorize("hasRole('GESTOR')")
    public Page<OcorrenciaResposta> excecoes(@ParameterObject @PageableDefault(size = 20) Pageable paginacao) {
        return excecoes.listar(paginacao);
    }

    @Operation(summary = "Emite a OS", description = "Sem corpo: executor e prazo do contrato da categoria.")
    @PostMapping("/{id}/emitir-os")
    @PreAuthorize("hasRole('GESTOR')")
    public OcorrenciaResposta emitirOS(@PathVariable Long id,
            @Valid @RequestBody(required = false) EmissaoOSRequisicao req) {
        return excecoes.emitirOS(id, req);
    }

    @Operation(summary = "Encaminha a outro órgão", description = "Registra o órgão e o protocolo externo.")
    @PostMapping("/{id}/encaminhar")
    @PreAuthorize("hasRole('GESTOR')")
    public OcorrenciaResposta encaminhar(@PathVariable Long id, @Valid @RequestBody EncaminhamentoRequisicao req) {
        return excecoes.encaminhar(id, req);
    }

    @Operation(summary = "Descarta a ocorrência", description = "Justificativa obrigatória.")
    @PostMapping("/{id}/descartar")
    @PreAuthorize("hasRole('GESTOR')")
    public OcorrenciaResposta descartar(@PathVariable Long id, @Valid @RequestBody JustificativaRequisicao req) {
        return excecoes.descartar(id, req.justificativa());
    }
}
