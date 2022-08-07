# Quarkus-Camel with Debezium and Infinispan

## Project for Leaning  -  It's not intended to be used in production

This project is only for apply some concepts of using the framework Apáche-Camel in a Quarkus project for sourcing from a Change Data Capture provided by Debezium engine and sink into a Infinispan cluster.

There is no implementation of secutity protocols, just because the matter of proof of concept this project has been based.

## Components

1. Postgres container configured to use [logical decoding](https://www.postgresql.org/docs/current/logicaldecoding-explanation.html), following [Debezium Postgres Connector recomendation](https://debezium.io/documentation/reference/stable/connectors/postgresql.html) with a adminer for UI with the databases.

2. Infinispan container (in a cluster of 3).

3. Quarkus component using Camel with Debezium e Infinispan extensions.

4. Monitoring stack with prometheus and grafana containers

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
    "ts_atl" timestamp DEFAULT now() NOT NULL,
    CONSTRAINT "TabelaCDC_pkey" PRIMARY KEY ("cd_cli")
) WITH (oids = false);


DROP TABLE IF EXISTS "SalarioCDC";
CREATE TABLE "public"."SalarioCDC" (
    "cd_cli" integer NOT NULL,
    "AnoMes" integer NOT NULL,
    "renda" numeric(15,2) NOT NULL,
    "ts_atl" timestamp DEFAULT now() NOT NULL,
    CONSTRAINT "SalarioCDC_cd_cli_AnoMes" PRIMARY KEY ("cd_cli", "AnoMes")
) WITH (oids = false);


ALTER TABLE ONLY "public"."SalarioCDC" ADD CONSTRAINT "SalarioCDC_cd_cli_fkey" FOREIGN KEY (cd_cli) REFERENCES "ClienteCDC"(cd_cli) ON UPDATE CASCADE ON DELETE CASCADE NOT DEFERRABLE;
```
3. Running the source-sink camel quarkus
* Go to Quarkus-kml-postgres2infinispan folder and execute the application
```shell script
cd quarkus-kml-postgres2infinispan
./mvnw compile quarkus:dev
```
* Any additional documentation of this project is [here](./quarkus-kml-postgres2infinispan/README.md)

4. Running monitoring stack
* Go to [monitoracao](./monitoracao) folder and run docker-compose
```shell script
cd monitoracao
docker-compose up -d
cd ..
```
The prometheus can be accessed at http://localhost:9090 and grafana accessed at http://localhost:3000 with "admin" and "password" as login/pswd credentials

For the first login in grafana, import [this](./monitoracao/infinispan-grafana.json) dashboard .

## Working
Once the application is up, you can use the Adminer UI (localhost:8080) to add, update and delete rows and see how it appears in the infinispan web console (localhost:11222).

### Generating events in Postres Database

Insert command in Postgres Database to observe cache updates:
```
WITH RANDOMCODE AS (
   SELECT DISTINCT       floor(random() * 9999 + 1) AS cd_cli
   FROM generate_series(1, < NUMBER OF ROWS TO INSERT >)
)
INSERT INTO "ClienteCDC" (cd_cli, nm_cli, cd_cpf)  (
    SELECT
       RANDOMCODE.cd_cli,
       'Cliente '  || md5(random()::text),
       floor(random() * 99999999999 + 1) 
    FROM RANDOMCODE
) ON CONFLICT (cd_cli) DO 
    UPDATE SET nm_cli = 'Cliente '|| "ClienteCDC".cd_cli || ' Atualizado',
               ts_atl = now();
```

and 

```
WITH cliente AS (
SELECT cd_cli FROM "ClienteCDC" WHERE random() < 0.25 limit 1000
)
INSERT INTO "SalarioCDC" ("cd_cli", "AnoMes", "renda") (
    SELECT 
       cliente.cd_cli,
       (2022 - floor(random() * 40))*100 +  floor(random() * 12 ),
       round((random()::decimal * 99999 + 1) , 2) 
    FROM generate_series(1, < NUMBER OF ROWS TO INSERT >), cliente

) ON CONFLICT ON CONSTRAINT "SalarioCDC_cd_cli_AnoMes" DO NOTHING
```

## Backlog
### In this project
1. Enhance metrics labeling in prometheus and adjust grafana dashboard
2. Automate setting-up project

### Complement of project
1. Another component to listen to Infinispan events of the 'cliente' cache and calculaing how long it takes to go from table to cache using this approach.
2. Implement kubernetes version of running