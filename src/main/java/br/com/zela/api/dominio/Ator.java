package br.com.zela.api.dominio;

/**
 * Quem executou uma ação na OS: um usuário logado ou o próprio sistema (emissão automática, agendadores).
 *
 * @param usuario usuário que agiu, ou {@code null} quando é o sistema
 * @param nome    nome exibido na timeline
 */
public record Ator(Usuario usuario, String nome) {

    public static Ator de(Usuario usuario) {
        return new Ator(usuario, usuario.getNome());
    }

    /** Ação automática, com um rótulo para a timeline (ex.: "Sistema (emissão automática)"). */
    public static Ator sistema(String rotulo) {
        return new Ator(null, rotulo);
    }

    public static Ator sistema() {
        return sistema("Sistema");
    }
}
