package br.com.zela.api.mensageria;

import br.com.zela.api.dominio.evento.EventoDominio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica os eventos de domínio só DEPOIS do commit: nunca avisamos sobre algo que deu rollback.
 *
 * <ul>
 *   <li>modo {@code rabbitmq} (padrão): envia ao exchange {@code zela.eventos} com a chave do evento
 *       (ex.: "os.escalada"); os consumidores processam de forma assíncrona;</li>
 *   <li>modo {@code local}: processa na hora, no mesmo processo. Usado nos testes e para rodar sem broker.</li>
 * </ul>
 */
@Component
public class PublicadorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorDeEventos.class);

    private final RabbitTemplate rabbit;
    private final ProcessadorDeEventos processador;
    private final boolean usarRabbit;

    public PublicadorDeEventos(RabbitTemplate rabbit, ProcessadorDeEventos processador,
            @Value("${zela.mensageria.modo:rabbitmq}") String modo) {
        this.rabbit = rabbit;
        this.processador = processador;
        this.usarRabbit = "rabbitmq".equalsIgnoreCase(modo);
        log.info("Mensageria: {}", usarRabbit ? "RabbitMQ (exchange " + RabbitConfig.EXCHANGE + ")" : "local (sem broker)");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConfirmar(EventoDominio evento) {
        if (!usarRabbit) {
            processador.notificar(evento);
            processador.alertar(evento);
            return;
        }
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, evento.tipo(), evento);
            log.debug("Evento publicado: {} (OS {}, ocorrência {})", evento.tipo(), evento.osId(), evento.ocorrenciaId());
        } catch (AmqpException e) {
            // A operação já foi gravada; só o aviso se perde. Fica registrado para reprocessar se preciso.
            log.error("RabbitMQ indisponível: evento {} não publicado (OS {}, ocorrência {}): {}",
                    evento.tipo(), evento.osId(), evento.ocorrenciaId(), e.getMessage());
        }
    }
}
