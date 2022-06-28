/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package br.com.als;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;

import br.com.als.schema.ClienteCDC;
import br.com.als.schema.SalarioCDC;

public class AggregateStore {

    static String PROP_AGGREGATE = "aggregate";

    public AggregateStore() {
    }

    public void readFromStoreAndUpdateIfNeeded(Exchange exchange) {
        final ClienteCDC clienteCDC = exchange.getMessage().getBody(ClienteCDC.class);
        final ProducerTemplate send = exchange.getContext().createProducerTemplate();

        // ClienteCDC aggregate = send.requestBody(Rotas.ROUTE_GET_AGGREGATE, clienteCDC.getCd_cli(), ClienteCDC.class);
        // if (aggregate == null) {
            // aggregate = clienteCDC;
        // }
        updateAggregate(exchange, clienteCDC, send);
        exchange.getMessage().setBody(clienteCDC);
    }

    public void readFromStoreAndAddSalario(Exchange exchange) {
        final SalarioCDC salario = exchange.getMessage().getBody(SalarioCDC.class);

        final ProducerTemplate send = exchange.getContext().createProducerTemplate();

        ClienteCDC aggregate = send.requestBody(Rotas.ROUTE_GET_AGGREGATE, salario.getCd_cli(), ClienteCDC.class);
        aggregate.addOrUpdateSalario(salario);

        updateAggregate(exchange, aggregate, send);
        exchange.getMessage().setBody(salario);
    }

    private void updateAggregate(Exchange exchange, final ClienteCDC aggregate, final ProducerTemplate send) {
        send.sendBody(Rotas.ROUTE_WRITE_AGGREGATE, aggregate);
        exchange.setProperty(PROP_AGGREGATE, aggregate);
    }
}
