package br.com.zela.api.mensageria;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.zela.api.dominio.StatusOS;
import br.com.zela.api.dominio.evento.EventoDominio;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/** Caminho do RabbitMQ (o broker é simulado): exchange, chave de roteamento e tolerância a falha. */
class PublicadorDeEventosTest {

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final ProcessadorDeEventos processador = mock(ProcessadorDeEventos.class);
    private final EventoDominio escalada =
            EventoDominio.daOS(4L, 4L, StatusOS.EMITIDA, StatusOS.ESCALADA, "Sistema (prazos)", "Prazo estourado");

    @Test
    void modoRabbitPublicaNoExchangeComAChaveDoEvento() {
        new PublicadorDeEventos(rabbit, processador, "rabbitmq").aoConfirmar(escalada);

        verify(rabbit).convertAndSend(RabbitConfig.EXCHANGE, "os.escalada", escalada);
        verifyNoInteractions(processador);
    }

    @Test
    void brokerForaDoArNaoDerrubaAOperacao() {
        doThrow(new AmqpConnectException(new java.net.ConnectException("Connection refused")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> new PublicadorDeEventos(rabbit, processador, "rabbitmq").aoConfirmar(escalada))
                .doesNotThrowAnyException();
    }

    @Test
    void modoLocalProcessaSemBroker() {
        new PublicadorDeEventos(rabbit, processador, "local").aoConfirmar(escalada);

        verify(processador).notificar(escalada);
        verify(processador).alertar(escalada);
        verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
