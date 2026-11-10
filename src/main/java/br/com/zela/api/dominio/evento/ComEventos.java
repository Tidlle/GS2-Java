package br.com.zela.api.dominio.evento;

import java.util.List;

/** Entidade que acumula eventos de domínio até ser gravada no banco. */
public interface ComEventos {

    /** Devolve os eventos pendentes e limpa a lista (cada evento é publicado uma única vez). */
    List<EventoDominio> retirarEventos();
}
