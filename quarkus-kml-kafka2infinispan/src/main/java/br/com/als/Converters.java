/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package br.com.als;

import java.util.HashSet;
import java.util.Set;

import org.apache.camel.Converter;
import org.apache.kafka.connect.data.Struct;
import org.jboss.logging.Logger;

import br.com.als.schema.ClienteCDC;
import br.com.als.schema.SalarioCDC;
import br.com.als.schema.SalariosOrfaos;
import io.vertx.core.json.JsonObject;

@Converter
public class Converters {
    static final Set<String> OP_CREATE_UPDATE = Set.of("u","r","c");
    private static final Logger LOGGER = Logger.getLogger(Converters.class);

    @Converter
    public static ClienteCDC clienteFromStruct(String structSTR) {
        JsonObject struct = new JsonObject(structSTR);
        String operation = struct.getJsonObject("payload").getString("op");
        LOGGER.debug("Operação:"+operation);
        if (OP_CREATE_UPDATE.contains(operation)){
            JsonObject record = struct.getJsonObject("payload").getJsonObject("after");
            return new ClienteCDC(record.getInteger("cd_cli"), record.getString("nm_cli"),
            Long.parseLong(record.getString("cd_cpf")),
            record.getLong("ts_atl"), null);
        } else {
            JsonObject record = struct.getJsonObject("payload").getJsonObject("before");
            LOGGER.trace("Record:"+record);
            return new ClienteCDC(record.getInteger("cd_cli"), "", 0L, 0L, null);
        }
    }

    @Converter
    public static SalarioCDC salarioFromStruct(String structSTR) {
        JsonObject struct = new JsonObject(structSTR);
        String operation = struct.getJsonObject("payload").getString("op");
        LOGGER.debug("Operação:"+operation);
        if (OP_CREATE_UPDATE.contains(operation)){
            JsonObject record = struct.getJsonObject("payload").getJsonObject("after");
            return new SalarioCDC(record.getInteger("cd_cli"), record.getInteger("AnoMes"),
                Float.parseFloat(record.getString("renda")),
                record.getLong("ts_atl"));
        } else {
            JsonObject record = struct.getJsonObject("payload").getJsonObject("before");
            LOGGER.trace("Record:"+record);
            return new SalarioCDC(record.getInteger("cd_cli"), record.getInteger("AnoMes"), 0.0f, 0L);            
        }

    }

    @Converter
    public static SalariosOrfaos salarioOrfaoFromStruct(Struct struct) {
        Set<SalarioCDC> salarios = new HashSet<SalarioCDC>();
        for (Object s : struct.getArray("salarios")) {
            SalarioCDC sal = new SalarioCDC(((Struct)s).getInt32("cd_cli"), 
                                 ((Struct)s).getInt32("AnoMes"), 
                                 ((Number) ((Struct)s).get("renda")).floatValue(),
                                 ((Struct)s).getInt64("ts_atl"));
            salarios.add(sal);
        }

        return new SalariosOrfaos(struct.getInt32("cd_cli"), salarios);
    }
}
