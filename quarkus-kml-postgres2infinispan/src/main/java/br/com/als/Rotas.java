package br.com.als;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.component.debezium.DebeziumConstants;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.debezium.data.Envelope;
import br.com.als.schema.ClienteCDC;
import br.com.als.schema.SalarioCDC;

@ApplicationScoped
public class Rotas extends EndpointRouteBuilder {

    @ConfigProperty(name = "camel.debezium.postgres.uri", defaultValue="x")
    private  String postgresUri;

    @ConfigProperty(name = "camel.debezium.postgres.username")
    private  String postgresUsername;

    @ConfigProperty(name = "camel.debezium.postgres.password")
    private  String postgresPassword;


    private static final String EVENT_TYPE_SALARIO = ".SalarioCDC";
    private static final String EVENT_TYPE_CLIENTE = ".ClienteCDC";

    static final String ROUTE_GET_AGGREGATE = "direct:getCliente";
    static final String ROUTE_WRITE_AGGREGATE = "direct:writeCliente";

    private final String ROUTE_STORE_CLIENTE_AGGREGATE = "infinispan://"+ ConfigProvider.getConfig()
    .getOptionalValue("camel.infinispan.cache.cliente", String.class).orElse("cliente");

    @Override
    public void configure() throws Exception {
        // from(platformHttp("/camel/hello"))
        // .setBody().simple("Camel runs on ${hostname}")
        // .log("Esse é o Bode:${body}")
        // .to(log("hi").showExchangePattern(true).showBodyType(false));

        final Predicate isCreateOrUpdateEvent = header(DebeziumConstants.HEADER_OPERATION).in(
                constant(Envelope.Operation.READ.code()),
                constant(Envelope.Operation.CREATE.code()),
                constant(Envelope.Operation.UPDATE.code()));

        final Predicate isClienteEvent = header(DebeziumConstants.HEADER_IDENTIFIER).endsWith(EVENT_TYPE_CLIENTE);

        final Predicate isSalarioEvent = header(DebeziumConstants.HEADER_IDENTIFIER).endsWith(EVENT_TYPE_SALARIO);

        final AggregateStore store = new AggregateStore();

        from(ROUTE_GET_AGGREGATE)
                .routeId(Rotas.class.getSimpleName() + ".BuscaCliente")
                .setHeader(InfinispanConstants.KEY).body()
                .setHeader(InfinispanConstants.OPERATION).constant("GET")
                .to(ROUTE_STORE_CLIENTE_AGGREGATE)
                .filter(body().isNotNull())
                // .unmarshal().json(JsonLibrary.Jackson, ClienteCDC.class)
                .log(LoggingLevel.TRACE, "Unarshalled question ${body}");

        from(ROUTE_WRITE_AGGREGATE)
                .routeId(Rotas.class.getSimpleName() + ".SalvaCliente")
                .setHeader(InfinispanConstants.KEY).simple("${body.cd_cli}")
                .log(LoggingLevel.TRACE, "About to marshall ${body}")
                // .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.TRACE, "Marshalled question ${body}")
                .setHeader(InfinispanConstants.VALUE).body()
                .to(ROUTE_STORE_CLIENTE_AGGREGATE);

        from(debeziumPostgres(
                postgresUri+"?"
                +"offsetStorageFileName=./offset-file-1.dat"
                +"&slotName=kml2dg"
                +"&databaseHostname=172.25.0.1"
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
                    .filter(isCreateOrUpdateEvent)
                        .convertBodyTo(ClienteCDC.class)
                        .log(LoggingLevel.TRACE, "Convertido para classe ${body}")
                        .bean(store, "readFromStoreAndUpdateIfNeeded")
                        // .process(new Processador())
                        // .to(ROUTE_MAIL_QUESTION_CREATE)
                    .endChoice()
                .when(isSalarioEvent)
                    .filter(isCreateOrUpdateEvent)
                        .convertBodyTo(SalarioCDC.class)
                        .log(LoggingLevel.TRACE, "Convertido para classe ${body}")
                        .bean(store,"readFromStoreAndAddSalario")
                    .endChoice()
                .otherwise()
                    .log(LoggingLevel.WARN, "Unknown type ${headers[" + DebeziumConstants.HEADER_IDENTIFIER + "]}")
            .endParent();

    }

}
