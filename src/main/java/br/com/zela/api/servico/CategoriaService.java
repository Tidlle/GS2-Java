package br.com.zela.api.servico;

import br.com.zela.api.dominio.Categoria;
import br.com.zela.api.dominio.Executor;
import br.com.zela.api.excecao.ConflitoException;
import br.com.zela.api.excecao.RecursoNaoEncontradoException;
import br.com.zela.api.repositorio.CategoriaRepository;
import br.com.zela.api.repositorio.OcorrenciaRepository;
import br.com.zela.api.web.dto.CategoriaRequisicao;
import br.com.zela.api.web.dto.CategoriaResposta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de categorias: cada uma liga um tipo de problema ao executor e ao prazo do contrato. */
@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final OcorrenciaRepository ocorrencias;
    private final ExecutorService executores;

    public CategoriaService(CategoriaRepository categorias, OcorrenciaRepository ocorrencias,
            ExecutorService executores) {
        this.categorias = categorias;
        this.ocorrencias = ocorrencias;
        this.executores = executores;
    }

    @Transactional(readOnly = true)
    public Page<CategoriaResposta> listar(Pageable paginacao) {
        return categorias.findAll(paginacao).map(CategoriaResposta::de);
    }

    @Transactional(readOnly = true)
    public CategoriaResposta buscar(Long id) {
        return CategoriaResposta.de(carregar(id));
    }

    @Transactional
    public CategoriaResposta criar(CategoriaRequisicao req) {
        String nome = req.nome().trim();
        if (categorias.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Já existe uma categoria com o nome " + nome);
        }
        Executor responsavel = executores.carregar(req.executorResponsavelId());
        Categoria nova = new Categoria(nome, req.icone().trim(), responsavel, req.prazoHoras(),
                req.emissaoAutomatica(), req.riscoBase());
        return CategoriaResposta.de(categorias.save(nova));
    }

    @Transactional
    public CategoriaResposta atualizar(Long id, CategoriaRequisicao req) {
        Categoria c = carregar(id);
        String nome = req.nome().trim();
        if (!c.getNome().equalsIgnoreCase(nome) && categorias.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Já existe uma categoria com o nome " + nome);
        }
        c.setNome(nome);
        c.setIcone(req.icone().trim());
        c.setExecutorResponsavel(executores.carregar(req.executorResponsavelId()));
        c.setPrazoHoras(req.prazoHoras());
        c.setEmissaoAutomatica(req.emissaoAutomatica());
        c.setRiscoBase(req.riscoBase());
        return CategoriaResposta.de(c);
    }

    /** Categoria já usada em ocorrências não pode ser excluída (409), para não perder o histórico. */
    @Transactional
    public void excluir(Long id) {
        Categoria c = carregar(id);
        if (ocorrencias.existsByCategoriaId(id)) {
            throw new ConflitoException("Categoria já usada em ocorrências e não pode ser excluída");
        }
        categorias.delete(c);
    }

    Categoria carregar(Long id) {
        return categorias.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
    }
}
