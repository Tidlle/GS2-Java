package br.com.zela.api.servico;

import br.com.zela.api.dominio.Executor;
import br.com.zela.api.excecao.ConflitoException;
import br.com.zela.api.excecao.RecursoNaoEncontradoException;
import br.com.zela.api.repositorio.CategoriaRepository;
import br.com.zela.api.repositorio.ExecutorRepository;
import br.com.zela.api.repositorio.OrdemServicoRepository;
import br.com.zela.api.repositorio.UsuarioRepository;
import br.com.zela.api.web.dto.ExecutorRequisicao;
import br.com.zela.api.web.dto.ExecutorResposta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** CRUD de executores (equipes próprias, empresas e concessionárias). */
@Service
public class ExecutorService {

    private final ExecutorRepository executores;
    private final CategoriaRepository categorias;
    private final OrdemServicoRepository ordens;
    private final UsuarioRepository usuarios;

    public ExecutorService(ExecutorRepository executores, CategoriaRepository categorias,
            OrdemServicoRepository ordens, UsuarioRepository usuarios) {
        this.executores = executores;
        this.categorias = categorias;
        this.ordens = ordens;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public Page<ExecutorResposta> listar(String nome, Pageable paginacao) {
        Page<Executor> pagina = StringUtils.hasText(nome)
                ? executores.findByNomeContainingIgnoreCase(nome.trim(), paginacao)
                : executores.findAll(paginacao);
        return pagina.map(ExecutorResposta::de);
    }

    @Transactional(readOnly = true)
    public ExecutorResposta buscar(Long id) {
        return ExecutorResposta.de(carregar(id));
    }

    @Transactional
    public ExecutorResposta criar(ExecutorRequisicao req) {
        Executor novo = new Executor(req.nome().trim(), req.tipo(), req.regiao().trim(), req.contrato().trim());
        return ExecutorResposta.de(executores.save(novo));
    }

    @Transactional
    public ExecutorResposta atualizar(Long id, ExecutorRequisicao req) {
        Executor e = carregar(id);
        e.setNome(req.nome().trim());
        e.setTipo(req.tipo());
        e.setRegiao(req.regiao().trim());
        e.setContrato(req.contrato().trim());
        return ExecutorResposta.de(e);
    }

    /** Só remove executor sem histórico: categorias, OS e usuários ligados a ele impedem a exclusão (409). */
    @Transactional
    public void excluir(Long id) {
        Executor e = carregar(id);
        if (categorias.existsByExecutorResponsavelId(id)) {
            throw new ConflitoException("Executor é responsável por categorias; troque o responsável antes de excluir");
        }
        if (ordens.existsByExecutorId(id)) {
            throw new ConflitoException("Executor tem ordens de serviço no histórico e não pode ser excluído");
        }
        if (!usuarios.findByExecutorId(id).isEmpty()) {
            throw new ConflitoException("Executor tem usuários vinculados e não pode ser excluído");
        }
        executores.delete(e);
    }

    Executor carregar(Long id) {
        return executores.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Executor", id));
    }
}
