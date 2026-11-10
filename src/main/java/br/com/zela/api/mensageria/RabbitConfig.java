package br.com.zela.api.mensageria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia no RabbitMQ (requisito 5, opção A):
 *
 * <pre>
 *                      ┌─ "os.*", "ocorrencia.*" ──► zela.notificacoes ─► NotificacaoConsumer (avisos no app)
 * zela.eventos (topic) ┤
 *                      └─ "os.escalada", "ocorrencia.urgente" ──► zela.alertas ─► AlertaConsumer (alertas)
 *
 * Mensagem que falha 3 vezes vai para zela.eventos.dlx → a .dlq da própria fila (não se perde nem trava a fila).
 * </pre>
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "zela.eventos";
    public static final String EXCHANGE_DLX = "zela.eventos.dlx";
    public static final String FILA_NOTIFICACOES = "zela.notificacoes";
    public static final String FILA_ALERTAS = "zela.alertas";

    @Bean
    MessageConverter conversorJson() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    Declarables topologiaZela() {
        TopicExchange eventos = new TopicExchange(EXCHANGE, true, false);
        DirectExchange dlx = new DirectExchange(EXCHANGE_DLX, true, false);

        Queue notificacoes = comDlq(FILA_NOTIFICACOES);
        Queue alertas = comDlq(FILA_ALERTAS);
        Queue notificacoesDlq = QueueBuilder.durable(FILA_NOTIFICACOES + ".dlq").build();
        Queue alertasDlq = QueueBuilder.durable(FILA_ALERTAS + ".dlq").build();

        return new Declarables(eventos, dlx, notificacoes, alertas, notificacoesDlq, alertasDlq,
                ligar(notificacoes, eventos, "os.*"),
                ligar(notificacoes, eventos, "ocorrencia.*"),
                ligar(alertas, eventos, "os.escalada"),
                ligar(alertas, eventos, "ocorrencia.urgente"),
                BindingBuilder.bind(notificacoesDlq).to(dlx).with(FILA_NOTIFICACOES),
                BindingBuilder.bind(alertasDlq).to(dlx).with(FILA_ALERTAS));
    }

    private static Queue comDlq(String nome) {
        return QueueBuilder.durable(nome).deadLetterExchange(EXCHANGE_DLX).deadLetterRoutingKey(nome).build();
    }

    private static Binding ligar(Queue fila, TopicExchange exchange, String chave) {
        return BindingBuilder.bind(fila).to(exchange).with(chave);
    }
}
