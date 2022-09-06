package br.com.als;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
// import org.apache.camel.component.debezium.DebeziumConstants;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.kafka.KafkaConstants;
import org.eclipse.microprofile.config.ConfigProvider;

import br.com.als.schema.ClienteCDC;
import br.com.als.schema.SalarioCDC;
// import io.debezium.data.Envelope;

@ApplicationScoped
public class Rotas extends EndpointRouteBuilder {

//     @ConfigProperty(name = "camel.debezium.postgres.uri", defaultValue="x")
//     private  String postgresUri;

//     @ConfigProperty(name = "camel.debezium.postgres.username")
//     private  String postgresUsername;

//     @ConfigProperty(name = "camel.debezium.postgres.password")
//     private  String postgresPassword;

//     @ConfigProperty(name = "camel.debezium.postgres.host")
//     private  String postgresHost;


    private static final String EVENT_TYPE_SALARIO = ".SalarioCDC";
    private static final String EVENT_TYPE_CLIENTE = ".ClienteCDC";

    static final String ROUTE_GET_CLIENTE = "direct:getCliente";
    static final String ROUTE_WRITE_CLIENTE = "direct:writeCliente";
    static final String ROUTE_REMOVE_CLIENTE = "direct:removeCliente";

    static final String ROUTE_GET_TEMP_SALARY = "direct:getSalario";
    static final String ROUTE_WRITE_TEMP_SALARY = "direct:writeSalario";
    static final String ROUTE_REMOVE_TEMP_SALARY = "direct:removeSalario";

    private final String ROUTE_STORE_CLIENTE_CACHE = "infinispan://"+ ConfigProvider.getConfig()
    .getOptionalValue("camel.infinispan.cache.cliente", String.class).orElse("cliente");

    private final String ROUTE_STORE_SALARY_TEMP_CACHE = "infinispan://"+ ConfigProvider.getConfig()
    .getOptionalValue("camel.infinispan.cache.salario-temp", String.class).orElse("salario-temp");
    
    @Override
    public void configure() throws Exception {
        // from(platformHttp("/camel/hello"))
        // .setBody().simple("Camel runs on ${hostname}")
        // .log("Esse é o Bode:${body}")
        // .to(log("hi").showExchangePattern(true).showBodyType(false));

        // final Predicate isCreateOrUpdateEvent = header(DebeziumConstants.HEADER_OPERATION).in(
        //         constant(Envelope.Operation.READ.code()).getExpression(),
        //         constant(Envelope.Operation.CREATE.code()).getExpression(),
        //         constant(Envelope.Operation.UPDATE.code()).getExpression());
 
        // final Predicate isDeleteEvent = header(DebeziumConstants.HEADER_OPERATION).in(
        //         constant(Envelope.Operation.TRUNCATE.code()).getExpression(),
        //         constant(Envelope.Operation.DELETE.code()).getExpression());
        
        
        final Predicate isClienteEvent = header(KafkaConstants.TOPIC).endsWith(EVENT_TYPE_CLIENTE);

        final Predicate isSalarioEvent = header(KafkaConstants.TOPIC).endsWith(EVENT_TYPE_SALARIO);

        final AggregateStore store = new AggregateStore();

        from(ROUTE_GET_CLIENTE)
                .routeId(Rotas.class.getSimpleName() + ".BuscaCliente")
                .setHeader(InfinispanConstants.KEY).body()
                .setHeader(InfinispanConstants.OPERATION).constant("GET")
                .to(ROUTE_STORE_CLIENTE_CACHE)
                .filter(body().isNotNull())
                // .unmarshal().json(JsonLibrary.Jackson, ClienteCDC.class)
                .log(LoggingLevel.TRACE, "Unarshalled question ${body}");

        from(ROUTE_WRITE_CLIENTE)
                .routeId(Rotas.class.getSimpleName() + ".SalvaCliente")
                .setHeader(InfinispanConstants.KEY).simple("${body.cd_cli}")
                .log(LoggingLevel.TRACE, "About to marshall ${body}")
                // .marshal().json(JsonLibrary.Jackson)
                .setHeader(InfinispanConstants.VALUE).body()
                .to(ROUTE_STORE_CLIENTE_CACHE);
 
        from(ROUTE_REMOVE_CLIENTE)
                .routeId(Rotas.class.getSimpleName() + ".RemoveCliente")
                .setHeader(InfinispanConstants.KEY).simple("${body}")
                .setHeader(InfinispanConstants.OPERATION).constant("REMOVE")
                // .setHeader(InfinispanConstants.RESULT_HEADER).constant("resultado")
                // .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.TRACE, "ToInfinispan: Removendo Cliente:${body}")
                // .setHeader(InfinispanConstants.VALUE).body()
                .to(ROUTE_STORE_CLIENTE_CACHE)
                .log(LoggingLevel.TRACE, "${headers} Body:${body} Result:${headerResult}");
                

        from(ROUTE_GET_TEMP_SALARY)
                .routeId(Rotas.class.getSimpleName() + ".BuscaSalarioTemp")
                .setHeader(InfinispanConstants.KEY).body()
                .setHeader(InfinispanConstants.OPERATION).constant("GET")
                // .setHeader(InfinispanConstants.KEY).simple("${headers.CamelInfinispanKey}")
                // .setHeader(InfinispanConstants.VALUE).body()
                .log(LoggingLevel.TRACE, "get Key:${headers.CamelInfinispanKey} / ValueKey:${headers.CamelInfinispanValue} / Body:${body}")
                // .marshal().json(JsonLibrary.Jackson)
                // .log(LoggingLevel.INFO, "Marshalled question ${headers} - ${body}");
                .to(ROUTE_STORE_SALARY_TEMP_CACHE)
                .filter(body().isNotNull())
                .log(LoggingLevel.TRACE, "retorno ${body}");

        from(ROUTE_WRITE_TEMP_SALARY)
                .routeId(Rotas.class.getSimpleName() + ".SalvaSalarioTemp")
                // .setHeader(InfinispanConstants.KEY).simple("${headers.CamelInfinispanKey}")
                .setHeader(InfinispanConstants.VALUE).body()
                .log(LoggingLevel.TRACE, "put Key:${headers.CamelInfinispanKey} / ValueKey:${headers.CamelInfinispanValue} / Body:${body}")
                // .marshal().json(JsonLibrary.Jackson)
                // .log(LoggingLevel.INFO, "Marshalled question ${headers} - ${body}");
                .to(ROUTE_STORE_SALARY_TEMP_CACHE);
                

        from(ROUTE_REMOVE_TEMP_SALARY)
                .routeId(Rotas.class.getSimpleName() + ".RemoveSalarioTemp")
                .setHeader(InfinispanConstants.KEY).simple("${body}")
                .setHeader(InfinispanConstants.OPERATION).constant("REMOVE")
                // .setHeader(InfinispanConstants.RESULT_HEADER).constant("resultado")
                // .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.TRACE, "ToInfinispan: Removendo SalarioOrfão do Cliente:${body}")
                // .setHeader(InfinispanConstants.VALUE).body()
                .to(ROUTE_STORE_SALARY_TEMP_CACHE)
                .log(LoggingLevel.TRACE, "${headers} ${body} ${headerResult}");

        from("kafka:"
              +"localhost-postgres.public.*"
              +"?brokers=localhost:9092"
              +"&topicIsPattern=true"
              +"&seekTo=BEGINNING"
              +"&consumersCount=3"
        //       +"&clientId=kmlKafka2Infinispan"
              +"&groupId=kmlKafka2Infinispan"
              )
              .routeId(Rotas.class.getSimpleName() + ".KafkaCDC")
              .log(LoggingLevel.TRACE, "Message received from Kafka : ${body}")
              .log(LoggingLevel.TRACE, "    on the topic ${headers[kafka.TOPIC]}")
        //       .log("    on the partition ${headers[kafka.PARTITION]}")
        //       .log("    with the offset ${headers[kafka.OFFSET]}")
        //       .log("    with the key ${headers[kafka.KEY]}")        
              .choice()
                .when(isClienteEvent)
                   .log(LoggingLevel.TRACE, "Tratando Cliente : ${headers[kafka.KEY]}}")
                    .convertBodyTo(ClienteCDC.class)
                    .log(LoggingLevel.TRACE, "Convertido para classe ${body}")
                    .bean(store, "readFromStoreAndUpdateIfNeeded")
                //TODO: Tratar deleção
                //   .choice()
                //   .when(isCreateOrUpdateEvent)
                //     .log(LoggingLevel.DEBUG, "Create/update: ${body} OP:${headers.CamelDebeziumOperation} ")
                //     .convertBodyTo(ClienteCDC.class)
                //     .log(LoggingLevel.DEBUG, "Convertido para classe ${body}")
                //     .bean(store, "readFromStoreAndUpdateIfNeeded")
                //   //   .endChoice()
                //   .when(isDeleteEvent)
                //     .log(LoggingLevel.DEBUG, "Deletando : KEY:${headers.CamelDebeziumKey} OP:${headers.CamelDebeziumOperation} ")
                //   //   .convertHeaderTo(ClienteCDC.class)
                //   //   .log(LoggingLevel.DEBUG, "Convertido para classe ${body}")
                //     .setBody(simple("${headers.CamelDebeziumKey}"))
                //     .bean(store, "deleteCliente")
                //   //   .endChoice()
                //   // .process(new Processador())
                //   // .to(ROUTE_MAIL_QUESTION_CREATE)
                //   .otherwise()
                //           .log(LoggingLevel.WARN, "Operação Não Identificada ${headers[" + DebeziumConstants.HEADER_IDENTIFIER + "]}")
                //           .endChoice()
               .when(isSalarioEvent)
                   .log(LoggingLevel.TRACE, "Tratando Salario : ${headers[kafka.KEY]}}")
          //       .filter(isCreateOrUpdateEvent)
                  .convertBodyTo(SalarioCDC.class)
                  .log(LoggingLevel.TRACE, "Convertido para classe ${body}")
                  .bean(store,"readFromStoreAndAddSalario")
        //       .endChoice()
          .otherwise()
              .log(LoggingLevel.WARN, "Unknown type ${headers[" + KafkaConstants.HEADERS + "]}")
      .endParent();



/*
        from(debeziumPostgres(
                postgresUri+"?"
                +"offsetStorageFileName=./offset-file-1.dat"
                +"offsetStoragePartitions=10"
                +"maxBatchSize=4096"
                +"&slotName=kml2dg"
                +"&databaseHostname="+postgresHost
                +"&databaseUser="+postgresUsername
                +"&databasePassword="+postgresPassword
                +"&databaseServerName=localhost-postgres"
                +"&databaseDbname=postgres"
                +"&databaseHistoryFileFilename=./history-file-1.dat"))
                .routeId(Rotas.class.getSimpleName() + ".CDCPostgres")
                .log(LoggingLevel.TRACE,"Evento do debezium:${headers.CamelDebeziumOperation} - BEFORE:${headers.CamelDebeziumBefore} AFTER: ${body}")
                .log(LoggingLevel.TRACE,"--> Para o infinispan: KEY:${headers.CamelDebeziumKey}  VALUE: ${body}")
                .choice()
                    .when(isClienteEvent)
                    //TODO: Tratar deleção
                        .choice()
                        .when(isCreateOrUpdateEvent)
                          .log(LoggingLevel.DEBUG, "Create/update: ${body} OP:${headers.CamelDebeziumOperation} ")
                          .convertBodyTo(ClienteCDC.class)
                          .log(LoggingLevel.DEBUG, "Convertido para classe ${body}")
                          .bean(store, "readFromStoreAndUpdateIfNeeded")
                        //   .endChoice()
                        .when(isDeleteEvent)
                          .log(LoggingLevel.DEBUG, "Deletando : KEY:${headers.CamelDebeziumKey} OP:${headers.CamelDebeziumOperation} ")
                        //   .convertHeaderTo(ClienteCDC.class)
                        //   .log(LoggingLevel.DEBUG, "Convertido para classe ${body}")
                          .setBody(simple("${headers.CamelDebeziumKey}"))
                          .bean(store, "deleteCliente")
                        //   .endChoice()
                        // .process(new Processador())
                        // .to(ROUTE_MAIL_QUESTION_CREATE)
                        .otherwise()
                                .log(LoggingLevel.WARN, "Operação Não Identificada ${headers[" + DebeziumConstants.HEADER_IDENTIFIER + "]}")
                                .endChoice()
                .when(isSalarioEvent)
                    .filter(isCreateOrUpdateEvent)
                        .convertBodyTo(SalarioCDC.class)
                        .log(LoggingLevel.DEBUG, "Convertido para classe ${body}")
                        .bean(store,"readFromStoreAndAddSalario")
                    .endChoice()
                .otherwise()
                    .log(LoggingLevel.WARN, "Unknown type ${headers[" + DebeziumConstants.HEADER_IDENTIFIER + "]}")
            .endParent();
*/
    }

}
