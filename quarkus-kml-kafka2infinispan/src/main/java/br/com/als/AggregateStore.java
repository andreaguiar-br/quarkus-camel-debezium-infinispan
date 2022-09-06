/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package br.com.als;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
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
        LOGGER.info("QT STATUS:"+ServiceStatus.values().length);
        for (ServiceStatus status : ServiceStatus.values()) {
            LOGGER.info("STATUS: "+status.ordinal()+"-"+status.name());

            
        }

    }

    public void readFromStoreAndUpdateIfNeeded(Exchange exchange) {
        ClienteCDC clienteFromBody = exchange.getMessage().getBody(ClienteCDC.class);
        final ProducerTemplate send = exchange.getContext().createProducerTemplate();
        
        // if (clienteCDC == null){
        //     LOGGER.errorf("Cliente NULL no ExchangeID=%s da Rota=%s",exchange.getExchangeId(), exchange.getFromRouteId();
        //     return;
        // }
        
        ClienteCDC clienteFromCache = send.requestBody(Rotas.ROUTE_GET_CLIENTE, clienteFromBody.getCd_cli(), ClienteCDC.class);
        if (clienteFromCache == null) {
        
            //cliente novo no cache - linka com cliente do body (fica com mesma apontador de memoria)
            clienteFromCache = clienteFromBody;
        
            SalariosOrfaos salariosOrfaos = send.requestBody(Rotas.ROUTE_GET_TEMP_SALARY, clienteFromBody.getCd_cli(), SalariosOrfaos.class);
            if (salariosOrfaos != null) {
                LOGGER.debug("Salarios Orfaos acharam o pai : "+salariosOrfaos);
                clienteFromCache.setSalarios(salariosOrfaos.getSalarios());
                send.sendBody(Rotas.ROUTE_REMOVE_TEMP_SALARY, clienteFromBody.getCd_cli());
            }
            updateCacheCliente(exchange, clienteFromCache, send);
            exchange.getMessage().setBody(clienteFromCache);
        
        } else {

            //atualiza o cliente do cache
            clienteFromBody.setSalarios(clienteFromCache.getSalarios()); //atualiza cliente com salarios já incluídos
            updateCacheCliente(exchange, clienteFromBody, send);
            exchange.getMessage().setBody(clienteFromBody);
       }
    }

    public void deleteCliente(Exchange exchange) {
        final Integer codCliente = exchange.getMessage().getBody(Struct.class).getInt32("cd_cli");
        final ProducerTemplate send = exchange.getContext().createProducerTemplate();

        LOGGER.debug("Removendo cliente : "+codCliente);
        
        send.sendBody(Rotas.ROUTE_REMOVE_CLIENTE, codCliente);
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

        ClienteCDC clienteFromCache = send.requestBody(Rotas.ROUTE_GET_CLIENTE, salario.getCd_cli(), ClienteCDC.class);
        LOGGER.trace("avaliando Cliente do cache :"+clienteFromCache);
        if (clienteFromCache != null) {
            clienteFromCache.addOrUpdateSalario(salario);
            updateCacheCliente(exchange, clienteFromCache, send);
        } else {
            this.addOrUpdateSalarioOrfao(salario, send);
        }
        exchange.getMessage().setBody(salario);
    }

    private void updateCacheCliente(Exchange exchange, final ClienteCDC cliente, final ProducerTemplate send) {
        send.sendBody(Rotas.ROUTE_WRITE_CLIENTE, cliente);
        exchange.setProperty(PROP_AGGREGATE, cliente);
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
