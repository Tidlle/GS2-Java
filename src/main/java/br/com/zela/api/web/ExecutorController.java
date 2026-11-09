package br.com.zela.api.web;

import br.com.zela.api.servico.ExecutorService;
import br.com.zela.api.web.dto.ExecutorRequisicao;
import br.com.zela.api.web.dto.ExecutorResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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

@Tag(name = "Executores", description = "Equipes e empresas que executam as OS. Leitura: gestor e supervisor; escrita: gestor.")
@RestController
@RequestMapping("/executores")
@PreAuthorize("hasAnyRole('GESTOR', 'SUPERVISOR')")
public class ExecutorController {

    private final ExecutorService executores;

    public ExecutorController(ExecutorService executores) {
        this.executores = executores;
    }

    @Operation(summary = "Lista executores (paginado)")
    @GetMapping
    public Page<ExecutorResposta> listar(
            @Parameter(description = "Filtra pelo nome (contém)") @RequestParam(required = false) String nome,
            @ParameterObject @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
        return executores.listar(nome, paginacao);
    }

    @Operation(summary = "Busca um executor")
    @GetMapping("/{id}")
    public ExecutorResposta buscar(@PathVariable Long id) {
        return executores.buscar(id);
    }

    @Operation(summary = "Cadastra um executor")
    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<ExecutorResposta> criar(@Valid @RequestBody ExecutorRequisicao req) {
        ExecutorResposta criado = executores.criar(req);
        return ResponseEntity.created(URI.create("/executores/" + criado.id())).body(criado);
    }

    @Operation(summary = "Atualiza um executor")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public ExecutorResposta atualizar(@PathVariable Long id, @Valid @RequestBody ExecutorRequisicao req) {
        return executores.atualizar(id, req);
    }

    @Operation(summary = "Exclui um executor", description = "Recusa (409) se houver categorias, OS ou usuários ligados.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        executores.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
