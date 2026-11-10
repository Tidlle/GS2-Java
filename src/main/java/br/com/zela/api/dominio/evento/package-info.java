/**
 * Eventos de domínio: fatos que aconteceram (ex.: "a OS 4 foi escalada"). As entidades registram os eventos
 * ao mudar de status; depois do commit eles viram mensagens no RabbitMQ.
 */
package br.com.zela.api.dominio.evento;
