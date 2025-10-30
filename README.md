# Pedidos API - Clojure + Datomic + Kafka

Sistema de pedidos demonstrando conceitos de Clojure com múltiplos datastores.

## Pré-requisitos

```bash
# PostgreSQL
sudo -u postgres psql -c "CREATE DATABASE pedidos_db;"
psql -U postgres -d pedidos_db -f resources/schema.sql

# MongoDB
mongod --dbpath /data/db
mongosh < resources/mongodb_seed.js

# Kafka
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties
bin/kafka-topics.sh --create --topic pedidos-eventos --bootstrap-server localhost:9092
```

## Instalação

```bash
lein deps
```

## Executar testes

```bash
# Unitários
lein test pedidos-api.unit-test

# Integração (certifique-se que Kafka/Mongo/Postgres estão rodando)
lein test pedidos-api.integration-test
```

## Rodar API

```bash
lein run
# Servidor em http://localhost:3000
```

## Conceitos Clojure

**Imutabilidade**: Todas estruturas são imutáveis por padrão. `(assoc map :key val)` retorna novo mapa.

**Funções puras**: Sem efeitos colaterais. `(defn soma [a b] (+ a b))` sempre retorna mesmo resultado.

**Threading macros**: `->` (thread-first) e `->>` (thread-last) encadeiam chamadas legíveis.

**Lazy sequences**: `(map)`, `(filter)` não executam até serem consumidas.

**Destructuring**: `[{:keys [nome email]}]` extrai valores de mapas em parâmetros.

**Namespaces**: Cada arquivo é um namespace, usa `:require` para importar.

## Datomic

Banco imutável baseado em fatos (datoms). Schema flexível define atributos com tipos e cardinalidades. Queries em Datalog (lógica declarativa). Mantém histórico completo de mudanças. `(d/q)` para queries, `(d/transact)` para escrita.

## Kafka

Sistema de mensagens distribuído. Producer envia mensagens assíncronas. Consumer lê em grupos paralelos. Tópicos divididos em partitions para escalabilidade. Offset rastreia posição de leitura.

## Bancos

**Postgres**: SQL relacional, ACID garantido, para dados estruturados.

**MongoDB**: NoSQL documento, schema flexível, para logs/eventos.

**Datomic**: Temporal imutável, audit trail completo, queries relacionais.# clojure-datomic-kafka-study
