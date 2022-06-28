/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package br.com.als;

import org.apache.camel.Converter;
import org.apache.kafka.connect.data.Struct;
import br.com.als.schema.ClienteCDC;
import br.com.als.schema.SalarioCDC;

@Converter
public class Converters {

    @Converter
    public static ClienteCDC clienteFromStruct(Struct struct) {
        return new ClienteCDC(struct.getInt32("cd_cli"), struct.getString("nm_cli"),
                ((Number) struct.get("cd_cpf")).longValue(),
                struct.getInt64("ts_atl"), null);
    }

    @Converter
    public static SalarioCDC salarioFromStruct(Struct struct) {
        return new SalarioCDC(struct.getInt32("cd_cli"), struct.getInt32("AnoMes"), 
        ((Number) struct.get("renda")).floatValue(),
                struct.getInt64("ts_atl"));
    }
}
