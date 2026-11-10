package br.com.zela.api.dominio.evento;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Listener JPA: quando a entidade é gravada (insert ou update), publica os eventos pendentes no Spring.
 * Assim nenhuma mudança de status escapa, mesmo as gravadas só pelo dirty checking do Hibernate.
 * O envio ao RabbitMQ acontece depois do commit (ver PublicadorDeEventos), nunca para um rollback.
 */
@Component
public class EventosDeDominioListener {

    private final ApplicationEventPublisher publicador;

    public EventosDeDominioListener(ApplicationEventPublisher publicador) {
        this.publicador = publicador;
    }

    @PostPersist
    @PostUpdate
    void publicar(Object entidade) {
        if (entidade instanceof ComEventos comEventos) {
            comEventos.retirarEventos().forEach(publicador::publishEvent);
        }
    }
}
