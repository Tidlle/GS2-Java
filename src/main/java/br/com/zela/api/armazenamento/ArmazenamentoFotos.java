package br.com.zela.api.armazenamento;

import br.com.zela.api.excecao.RecursoNaoEncontradoException;
import br.com.zela.api.excecao.RegraNegocioException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda as fotos em disco (pasta configurável, volume no container).
 * Segurança: o tipo é conferido pelos bytes do arquivo, o nome é gerado pelo servidor (UUID)
 * e a leitura só aceita nomes nesse formato, o que impede acesso a outros arquivos (path traversal).
 */
@Service
public class ArmazenamentoFotos {

    public static final String PREFIXO_URL = "/fotos/";
    private static final Pattern NOME_VALIDO = Pattern.compile("^[0-9a-f]{32}\\.(jpg|png|webp)$");

    private final Path diretorio;

    public ArmazenamentoFotos(@Value("${zela.fotos.diretorio:uploads}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.diretorio);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta de fotos " + this.diretorio, e);
        }
    }

    /** Salva a foto e devolve a URL pública relativa (ex.: /fotos/3f2a….jpg). */
    public String salvar(MultipartFile arquivo) {
        Imagem imagem = ler(arquivo);
        String nome = UUID.randomUUID().toString().replace("-", "") + "." + imagem.tipo().extensao;
        try {
            Files.write(diretorio.resolve(nome), imagem.dados());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar a foto", e);
        }
        return PREFIXO_URL + nome;
    }

    /** Lê e valida um upload sem gravar (usado também pela triagem). */
    public Imagem ler(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Envie a foto no campo 'arquivo'");
        }
        try {
            byte[] dados = arquivo.getBytes();
            Imagem.Tipo tipo = Imagem.detectar(dados)
                    .orElseThrow(() -> new RegraNegocioException("A foto deve ser uma imagem JPEG, PNG ou WEBP"));
            return new Imagem(dados, tipo);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler a foto enviada", e);
        }
    }

    /** Carrega pelo nome do arquivo (404 se o nome for inválido ou o arquivo não existir). */
    public Imagem carregar(String nome) {
        Matcher m = NOME_VALIDO.matcher(nome == null ? "" : nome);
        Path caminho = m.matches() ? diretorio.resolve(nome).normalize() : null;
        if (caminho == null || !caminho.startsWith(diretorio) || !Files.isRegularFile(caminho)) {
            throw new RecursoNaoEncontradoException("Foto", nome);
        }
        try {
            Imagem.Tipo tipo = Imagem.Tipo.daExtensao(m.group(1)).orElseThrow();
            return new Imagem(Files.readAllBytes(caminho), tipo);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler a foto", e);
        }
    }

    /** Carrega a partir da URL gravada na ocorrência/OS; vazio se não for uma foto deste servidor. */
    public Optional<Imagem> carregarPorUrl(String url) {
        if (url == null || !url.startsWith(PREFIXO_URL)) {
            return Optional.empty();
        }
        try {
            return Optional.of(carregar(url.substring(PREFIXO_URL.length())));
        } catch (RecursoNaoEncontradoException e) {
            return Optional.empty();
        }
    }
}
