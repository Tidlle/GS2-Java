package br.com.zela.api.armazenamento;

import java.util.Arrays;
import java.util.Optional;

/** Bytes de uma foto com o tipo detectado pelo conteúdo do arquivo (não pela extensão nem pelo cabeçalho). */
public record Imagem(byte[] dados, Tipo tipo) {

    public enum Tipo {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp");

        public final String mime;
        public final String extensao;

        Tipo(String mime, String extensao) {
            this.mime = mime;
            this.extensao = extensao;
        }

        public static Optional<Tipo> daExtensao(String extensao) {
            return Arrays.stream(values()).filter(t -> t.extensao.equalsIgnoreCase(extensao)).findFirst();
        }
    }

    /** Identifica o formato pelos primeiros bytes ("assinatura" do arquivo). */
    public static Optional<Tipo> detectar(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of(Tipo.JPEG);
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Optional.of(Tipo.PNG);
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of(Tipo.WEBP);
        }
        return Optional.empty();
    }
}
