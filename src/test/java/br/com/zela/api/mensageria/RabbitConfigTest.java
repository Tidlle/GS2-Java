package br.com.zela.api.mensageria;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.zela.api.dominio.StatusOS;
import br.com.zela.api.dominio.StatusOcorrencia;
import br.com.zela.api.dominio.evento.EventoDominio;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;

/** Confere a topologia declarada no RabbitMQ e as chaves de roteamento dos eventos. */
class RabbitConfigTest {

    private final Declarables topologia = new RabbitConfig().topologiaZela();

    private List<String> chavesDa(String fila) {
        return topologia.getDeclarablesByType(Binding.class).stream()
                .filter(b -> b.getDestination().equals(fila) && b.getExchange().equals(RabbitConfig.EXCHANGE))
                .map(Binding::getRoutingKey)
                .toList();
    }

    @Test
    void notificacoesRecebemTodosOsEventosEAlertasSoOsCriticos() {
        assertThat(chavesDa(RabbitConfig.FILA_NOTIFICACOES)).containsExactlyInAnyOrder("os.*", "ocorrencia.*");
        assertThat(chavesDa(RabbitConfig.FILA_ALERTAS)).containsExactlyInAnyOrder("os.escalada", "ocorrencia.urgente");
    }

    @Test
    void filasSaoDuraveisEMandamFalhasParaADlq() {
        Queue notificacoes = topologia.getDeclarablesByType(Queue.class).stream()
                .filter(q -> q.getName().equals(RabbitConfig.FILA_NOTIFICACOES)).findFirst().orElseThrow();
        assertThat(notificacoes.isDurable()).isTrue();
        assertThat(notificacoes.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitConfig.EXCHANGE_DLX)
                .containsEntry("x-dead-letter-routing-key", RabbitConfig.FILA_NOTIFICACOES);
        assertThat(topologia.getDeclarablesByType(Queue.class)).extracting(Queue::getName)
                .contains("zela.notificacoes.dlq", "zela.alertas.dlq");
    }

    @Test
    void chaveDeRoteamentoVemDoTipoEDoStatus() {
        assertThat(EventoDominio.daOS(4L, 4L, StatusOS.EMITIDA, StatusOS.ESCALADA, "Sistema", null).tipo())
                .isEqualTo("os.escalada");
        assertThat(EventoDominio.daOcorrencia(9L, StatusOcorrencia.REPORTADA, StatusOcorrencia.URGENTE, null).tipo())
                .isEqualTo("ocorrencia.urgente");
    }
}
