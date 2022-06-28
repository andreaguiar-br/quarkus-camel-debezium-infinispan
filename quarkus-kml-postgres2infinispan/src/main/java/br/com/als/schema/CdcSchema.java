package br.com.als.schema;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;


@AutoProtoSchemaBuilder(includeClasses= { ClienteCDC.class, SalarioCDC.class},
      schemaPackageName = "CDC")
public interface CdcSchema extends SerializationContextInitializer { 
} 
