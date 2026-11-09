package br.com.zela.api.web;

import br.com.zela.api.servico.CategoriaService;
import br.com.zela.api.servico.ExcecaoService;
import br.com.zela.api.web.dto.CategoriaRequisicao;
import br.com.zela.api.web.dto.CategoriaResposta;
import br.com.zela.api.web.dto.ExecutorResposta;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Categorias", description = "Tipos de problema com executor e prazo do contrato. Leitura: todos; escrita: gestor.")
@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categorias;
    private final ExcecaoService excecoes;

    public CategoriaController(CategoriaService categorias, ExcecaoService excecoes) {
        this.categorias = categorias;
        this.excecoes = excecoes;
    }

    @Operation(summary = "Lista categorias (paginado)")
    @GetMapping
    public Page<CategoriaResposta> listar(
            @ParameterObject @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
        return categorias.listar(paginacao);
    }

    @Operation(summary = "Busca uma categoria")
    @GetMapping("/{id}")
    public CategoriaResposta buscar(@PathVariable Long id) {
        return categorias.buscar(id);
    }

    @Operation(summary = "Executor sugerido", description = "Executor do contrato da categoria (usado na emissão manual).")
    @GetMapping("/{id}/executor-sugerido")
    @PreAuthorize("hasAnyRole('GESTOR', 'SUPERVISOR')")
    public ExecutorResposta executorSugerido(@PathVariable Long id) {
        return excecoes.executorSugerido(id);
    }

    @Operation(summary = "Cadastra uma categoria")
    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<CategoriaResposta> criar(@Valid @RequestBody CategoriaRequisicao req) {
        CategoriaResposta criada = categorias.criar(req);
        return ResponseEntity.created(URI.create("/categorias/" + criada.id())).body(criada);
    }

    @Operation(summary = "Atualiza uma categoria")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public CategoriaResposta atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequisicao req) {
        return categorias.atualizar(id, req);
    }

    @Operation(summary = "Exclui uma categoria", description = "Recusa (409) se a categoria já foi usada em ocorrências.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        categorias.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
