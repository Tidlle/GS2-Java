package br.com.zela.api.seguranca;

import br.com.zela.api.excecao.MuitasTentativasException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Proteção contra força bruta no login: depois de 5 senhas erradas para o mesmo e-mail em 15 minutos,
 * o login desse e-mail fica bloqueado até a janela acabar (HTTP 429). Um login certo zera a contagem.
 * Em memória: suficiente para uma instância; com várias réplicas, iria para um cache compartilhado.
 */
@Component
public class LimiteDeTentativasDeLogin {

    static final int MAXIMO_DE_FALHAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);
    private static final int LIMPAR_ACIMA_DE = 10_000;

    private final Map<String, Tentativas> porEmail = new ConcurrentHashMap<>();

    private record Tentativas(int falhas, Instant inicio) {
        boolean expirou() {
            return inicio.plus(JANELA).isBefore(Instant.now());
        }
    }

    public void verificar(String email) {
        Tentativas t = porEmail.get(chave(email));
        if (t != null && !t.expirou() && t.falhas() >= MAXIMO_DE_FALHAS) {
            long minutos = Math.max(1, Duration.between(Instant.now(), t.inicio().plus(JANELA)).toMinutes());
            throw new MuitasTentativasException(minutos);
        }
    }

    public void registrarFalha(String email) {
        if (porEmail.size() > LIMPAR_ACIMA_DE) {
            porEmail.values().removeIf(Tentativas::expirou);
        }
        porEmail.compute(chave(email), (k, t) -> t == null || t.expirou()
                ? new Tentativas(1, Instant.now())
                : new Tentativas(t.falhas() + 1, t.inicio()));
    }

    public void registrarSucesso(String email) {
        porEmail.remove(chave(email));
    }

    private static String chave(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
