/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package br.com.als;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.kafka.connect.data.Struct;
import org.jboss.logging.Logger;

import br.com.als.schema.ClienteCDC;
import br.com.als.schema.SalarioCDC;
import br.com.als.schema.SalariosOrfaos;

public class AggregateStore {

    static String PROP_AGGREGATE = "Cliente";
    private static final Logger LOGGER = Logger.getLogger(AggregateStore.class);

    public AggregateStore() {
    }

    public void readFromStoreAndUpdateIfNeeded(Exchange exchange) {
        final ClienteCDC clienteCDC = exchange.getMessage().getBody(ClienteCDC.class);
        final ProducerTemplate send = exchange.getContext().createProducerTemplate();
        
        // if (clienteCDC == null){
        //     LOGGER.errorf("Cliente NULL no ExchangeID=%s da Rota=%s",exchange.getExchangeId(), exchange.getFromRouteId();
        //     return;
        // }
        
        ClienteCDC aggregate = send.requestBody(Rotas.ROUTE_GET_AGGREGATE, clienteCDC.getCd_cli(), ClienteCDC.class);
        if (aggregate == null) {
        
            aggregate = clienteCDC;
        
            SalariosOrfaos salariosOrfaos = send.requestBody(Rotas.ROUTE_GET_TEMP_SALARY, clienteCDC.getCd_cli(), SalariosOrfaos.class);
            if (salariosOrfaos != null) {
                LOGGER.trace("Salarios Orfaos acharam o pai: : "+salariosOrfaos);
                aggregate.setSalarios(salariosOrfaos.getSalarios());
            }
    
        }
        updateAggregate(exchange, clienteCDC, send);
        exchange.getMessage().setBody(clienteCDC);
    }

    public void deleteCliente(Exchange exchange) {
        final Integer codCliente = exchange.getMessage().getBody(Struct.class).getInt32("cd_cli");
        final ProducerTemplate send = exchange.getContext().createProducerTemplate();

        LOGGER.debug("Removendo cliente : "+codCliente);
        
        send.sendBody(Rotas.ROUTE_REMOVE_AGGREGATE, codCliente);
        send.sendBody(Rotas.ROUTE_REMOVE_TEMP_SALARY, codCliente);
        exchange.setProperty(PROP_AGGREGATE, codCliente);

        // if (clienteCDC == null){
        //     LOGGER.errorf("Cliente NULL no ExchangeID=%s da Rota=%s",exchange.getExchangeId(), exchange.getFromRouteId();
        //     return;
        // }
        
        // ClienteCDC aggregate = send.requestBody(Rotas.ROUTE_REMOVE_AGGREGATE, codCliente, ClienteCDC.class);
        // if (aggregate == null) {
        
        //     aggregate = clienteCDC;
        
        //     SalariosOrfaos salariosOrfaos = send.requestBody(Rotas.ROUTE_GET_TEMP_SALARY, clienteCDC.getCd_cli(), SalariosOrfaos.class);
        //     if (salariosOrfaos != null) {
        //         LOGGER.trace("Salarios Orfaos acharam o pai: : "+salariosOrfaos);
        //         aggregate.setSalarios(salariosOrfaos.getSalarios());
        //     }
    
        // }
        // updateAggregate(exchange, clienteCDC, send);
        // exchange.getMessage().setBody(clienteCDC);
    }

    public void readFromStoreAndAddSalario(Exchange exchange) {
        final SalarioCDC salario = exchange.getMessage().getBody(SalarioCDC.class);

        final ProducerTemplate send = exchange.getContext().createProducerTemplate();

        ClienteCDC aggregate = send.requestBody(Rotas.ROUTE_GET_AGGREGATE, salario.getCd_cli(), ClienteCDC.class);
        LOGGER.trace("avaliando aggregate :"+aggregate);
        if (aggregate != null) {
            aggregate.addOrUpdateSalario(salario);
        } else {
            this.addOrUpdateSalarioOrfao(salario, send);
        }
        updateAggregate(exchange, aggregate, send);
        exchange.getMessage().setBody(salario);
    }

    private void updateAggregate(Exchange exchange, final ClienteCDC aggregate, final ProducerTemplate send) {
        send.sendBody(Rotas.ROUTE_WRITE_AGGREGATE, aggregate);
        exchange.setProperty(PROP_AGGREGATE, aggregate);
    }

    private void addOrUpdateSalarioOrfao(SalarioCDC salario, ProducerTemplate send) {
        SalariosOrfaos salariosOrfaos = send.requestBody(Rotas.ROUTE_GET_TEMP_SALARY, salario.getCd_cli(), SalariosOrfaos.class);
        LOGGER.trace("Salarios Orfaos: : "+salariosOrfaos);
        if (salariosOrfaos == null){
          salariosOrfaos = new SalariosOrfaos();
          salariosOrfaos.setCd_cli(salario.getCd_cli());
        }
        salariosOrfaos.addOrUpdateSalario(salario);
        LOGGER.trace("ENVIANDO TEMP-SALARY: : "+salariosOrfaos);
        send.sendBodyAndHeader(Rotas.ROUTE_WRITE_TEMP_SALARY, salariosOrfaos, InfinispanConstants.KEY, salario.getCd_cli());
  }

}
