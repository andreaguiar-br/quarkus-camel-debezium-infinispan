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
import br.com.als.schema.ClienteCDC;
import br.com.als.schema.SalarioCDC;
import br.com.als.schema.SalariosOrfaos;

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
