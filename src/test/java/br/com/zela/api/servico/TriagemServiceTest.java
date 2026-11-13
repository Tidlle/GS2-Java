package br.com.zela.api.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.zela.api.armazenamento.ArmazenamentoFotos;
import br.com.zela.api.armazenamento.Imagem;
import br.com.zela.api.dominio.Categoria;
import br.com.zela.api.dominio.Executor;
import br.com.zela.api.dominio.TipoExecutor;
import br.com.zela.api.ia.Comparacao;
import br.com.zela.api.ia.FalhaIAException;
import br.com.zela.api.ia.ServicoIA;
import br.com.zela.api.ia.Triagem;
import br.com.zela.api.repositorio.CategoriaRepository;
import br.com.zela.api.web.dto.TriagemResposta;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** Conferências feitas sobre a resposta da IA: categoria precisa existir, confiança entre 0 e 1, falha não derruba. */
class TriagemServiceTest {

    private final Executor executor = new Executor("Luz", TipoExecutor.EMPRESA, "Sul", "C1");
    private final List<Categoria> categorias = List.of(
            new Categoria("Iluminação", "bulb", executor, 48, true, 2),
            new Categoria("Árvore", "leaf", executor, 24, false, 4));
    private final ServicoIA ia = mock(ServicoIA.class);
    private final ArmazenamentoFotos fotos = mock(ArmazenamentoFotos.class);
    private final CategoriaRepository repositorio = mock(CategoriaRepository.class);
    private final TriagemService servico = new TriagemService(ia, fotos, repositorio);
    private final MockMultipartFile arquivo = new MockMultipartFile("arquivo", new byte[] {1});

    private TriagemResposta triarCom(Triagem respostaDaIa) {
        when(fotos.ler(any())).thenReturn(new Imagem(new byte[] {1}, Imagem.Tipo.PNG));
        when(fotos.salvar(any())).thenReturn("/fotos/x.png");
        when(repositorio.findAll()).thenReturn(categorias);
        when(ia.origem()).thenReturn("gemini");
        when(ia.triar(any(), any())).thenReturn(respostaDaIa);
        return servico.triar(arquivo);
    }

    @Test
    void aceitaCategoriaSemAcentoEMaiusculas() {
        TriagemResposta r = triarCom(new Triagem("ARVORE", "Galho sobre a via.", true, 0.9));
        assertThat(r.categoriaNome()).isEqualTo("Árvore");
        assertThat(r.urgente()).isTrue();
        assertThat(r.aviso()).contains("199").contains("193");
    }

    @Test
    void categoriaInventadaPelaIaNaoEhAceita() {
        TriagemResposta r = triarCom(new Triagem("Pichação", "Muro pichado.", false, 0.95));
        assertThat(r.categoriaId()).isNull();
        assertThat(r.fotoUrl()).isEqualTo("/fotos/x.png");
        assertThat(r.aviso()).contains("manualmente");
    }

    @Test
    void confiancaForaDaFaixaEhLimitadaEBaixaGeraAviso() {
        assertThat(triarCom(new Triagem("Iluminação", "Poste apagado.", false, 7)).confianca()).isEqualTo(1.0);
        TriagemResposta baixa = triarCom(new Triagem("Iluminação", "Poste apagado.", false, 0.3));
        assertThat(baixa.aviso()).contains("baixa confiança");
    }

    @Test
    void osNaoResolvidaNuncaTemConfiancaAlta() {
        // O Gemini chegou a responder "não resolvido, com 100% de certeza": isso NÃO pode virar confiança de resolução.
        when(fotos.carregarPorUrl(any())).thenReturn(Optional.of(new Imagem(new byte[] {1}, Imagem.Tipo.PNG)));
        when(ia.comparar(any(), any(), any())).thenReturn(new Comparacao(false, 1.0, "Imagens idênticas."));

        Comparacao c = servico.comparar("/fotos/antes.png", "/fotos/depois.png", "Buraco").orElseThrow();

        assertThat(c.resolvido()).isFalse();
        assertThat(c.confianca()).isZero();
    }

    @Test
    void osResolvidaMantemAConfiancaDaIa() {
        when(fotos.carregarPorUrl(any())).thenReturn(Optional.of(new Imagem(new byte[] {1}, Imagem.Tipo.PNG)));
        when(ia.comparar(any(), any(), any())).thenReturn(new Comparacao(true, 0.93, "Asfalto refeito."));

        assertThat(servico.comparar("/fotos/antes.png", "/fotos/depois.png", "Buraco").orElseThrow().confianca())
                .isEqualTo(0.93);
    }

    @Test
    void falhaDaIaNaoImpedeOReporte() {
        when(fotos.ler(any())).thenReturn(new Imagem(new byte[] {1}, Imagem.Tipo.PNG));
        when(fotos.salvar(any())).thenReturn("/fotos/x.png");
        when(repositorio.findAll()).thenReturn(categorias);
        when(ia.origem()).thenReturn("gemini");
        when(ia.triar(any(), any())).thenThrow(new FalhaIAException("cota excedida", null));

        TriagemResposta r = servico.triar(arquivo);
        assertThat(r.fotoUrl()).isEqualTo("/fotos/x.png");
        assertThat(r.categoriaId()).isNull();
        assertThat(r.aviso()).contains("não conseguiu analisar");
    }
}
