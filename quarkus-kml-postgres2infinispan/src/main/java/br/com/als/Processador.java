package br.com.als;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.jboss.logging.Logger;

public class Processador implements Processor {
    private static final Logger LOGGER = Logger.getLogger(Processador.class);

    public void process(Exchange exchange) throws Exception {
        Map<String,Object> headerValue = exchange.getIn().getHeaders();
        Struct bodyValue = exchange.getIn().getBody(Struct.class);
        Schema schemaValue = bodyValue.schema();

        LOGGER.info("Header ==> :" + headerValue);
        LOGGER.info("Tabela :" + ((Map)headerValue.get("CamelDebeziumSourceMetadata")).get("table"));
        LOGGER.info("Operação :" + headerValue.get("CamelDebeziumOperation"));
        LOGGER.info("Body value is :" + bodyValue);
        LOGGER.info("With Schema : " + schemaValue);
        LOGGER.info("And fields of :" + schemaValue.fields());
        LOGGER.info("Field ts has `" + schemaValue.field("ts_atl").schema() + "` type");
        LOGGER.info("Field name has `" + schemaValue.field("nm_cli").schema() + "` type");
        // List<Map<String, Object>> rows = exchange.getIn().getBody(List.class);
        //List<Key> keys = new ArrayList<Key>();
        // Key key = new Key();
        // for (Map<String, Object> row : rows) {
 
            // key.setId(((Number) row.get("ID")).intValue());
            // key.setKey((String) row.get("KEY"));
            // key.setValue((String) row.get("VALUE"));
            //keys.add(key);
        // }
        // exchange.getOut().setBody(key);
    }    
}
