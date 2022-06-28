# Quarkus-Camel com Debezium e Infinispan

## Project for Leaning  -  It's not intended to be used in production

This project is only for apply some concepts of using the framework Apáche-Camel in a Quarkus project for sourcing from a Change Data Capture provided by Debezium engine and sink into a Infinispan cluster.

There is no implementation of secutity protocols, just because the matter of proof of concept this project has been based.

## Components

1. Postgres container configured to use [logical decoding](https://www.postgresql.org/docs/current/logicaldecoding-explanation.html), following [Debezium Postgres Connector recomendation](https://debezium.io/documentation/reference/stable/connectors/postgresql.html) with a adminer for UI with the databases.

2. Infinispan container (in a cluster of 3).

3. Quarkus component using Camel with Debezium e Infinispan extensions.

## Running
1. Setting-up Infinispan container
* Go to Infinispan-docker folder and run docker-compose
```shell script
cd infinispan-docker
docker-compose up -d
cd ..
```
The infinspan admin console can be accessed at http://localhost:11222 with the credentials in [identities.batch](./infinispan-docker/user-config/identities.batch)

* Create a cache named 'cliente' with default configuration:
```
{
  "distributed-cache": {
    "mode": "SYNC",
    "encoding": {
      "media-type": "application/x-protostream"
    },
    "statistics": true
  }
}
```

2. Setting-up Postgres
* Go to Postgres-docker folder and run docker-compose
```shell script
cd postgres-docker
docker-compose up -d
cd ..
```

* Acess adminer UI at http://localhost:8080 with the credentials in the [docker-compose.yml](./postgres-docker/docker-compose.yml) 
* Select DB **postgres** and schema **public**
* Create option 'SQL Command' and execute the DDL:

```
DROP TABLE IF EXISTS "ClienteCDC";
DROP SEQUENCE IF EXISTS "TabelaCDC_cd_cli_seq";
CREATE SEQUENCE "TabelaCDC_cd_cli_seq" INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 6 CACHE 1;

CREATE TABLE "public"."ClienteCDC" (
    "cd_cli" integer DEFAULT nextval('"TabelaCDC_cd_cli_seq"') NOT NULL,
    "nm_cli" character varying(60) NOT NULL,
    "cd_cpf" numeric(11,0) NOT NULL,
    "ts_atl" timestamp NOT NULL,
    CONSTRAINT "TabelaCDC_pkey" PRIMARY KEY ("cd_cli")
) WITH (oids = false);


DROP TABLE IF EXISTS "SalarioCDC";
CREATE TABLE "public"."SalarioCDC" (
    "cd_cli" integer NOT NULL,
    "AnoMes" integer NOT NULL,
    "renda" numeric(15,2) NOT NULL,
    "ts_atl" timestamp NOT NULL,
    CONSTRAINT "SalarioCDC_cd_cli_AnoMes" PRIMARY KEY ("cd_cli", "AnoMes")
) WITH (oids = false);


ALTER TABLE ONLY "public"."SalarioCDC" ADD CONSTRAINT "SalarioCDC_cd_cli_fkey" FOREIGN KEY (cd_cli) REFERENCES "ClienteCDC"(cd_cli) ON UPDATE CASCADE ON DELETE RESTRICT NOT DEFERRABLE;
```
3. Go to Quarkus-kml-postgres2infinispan folder and execute the application
```shell script
cd quarkus-kml-postgres2infinispan
./mvnw compile quarkus:dev
```

## Working
Once the application is up, you can use the Adminer UI (localhost:8080) to add, update and delete rows and see how it appears in the infinispan web console (localhost:11222).

## Backlog

In my backlog, I'am planning to create another component to listen to Infinispan events of the 'cliente' cache and calculaing how log it took to go from table to cache using this approach.