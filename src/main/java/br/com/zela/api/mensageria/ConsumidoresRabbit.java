package br.com.zela.api.mensageria;

import br.com.zela.api.dominio.evento.EventoDominio;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumidores assíncronos das filas do RabbitMQ. Cada fila tem o seu consumidor:
 * a de notificações cria os avisos do app; a de alertas trata urgências e escalonamentos.
 * Só existem no modo {@code rabbitmq} (no modo local o publicador chama o processador direto).
 */
@Component
@ConditionalOnProperty(name = "zela.mensageria.modo", havingValue = "rabbitmq", matchIfMissing = true)
public class ConsumidoresRabbit {

    private final ProcessadorDeEventos processador;

    public ConsumidoresRabbit(ProcessadorDeEventos processador) {
        this.processador = processador;
    }

    @RabbitListener(queues = RabbitConfig.FILA_NOTIFICACOES)
    public void notificacoes(EventoDominio evento) {
        processador.notificar(evento);
    }

    @RabbitListener(queues = RabbitConfig.FILA_ALERTAS)
    public void alertas(EventoDominio evento) {
        processador.alertar(evento);
    }
}
