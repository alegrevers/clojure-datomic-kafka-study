(ns pedidos-api.datomic.db
  (:require [datomic.api :as d]
            [pedidos-api.datomic.schema :as schema]))

;; Conexão com Datomic (banco imutável, todas transações são preservadas)
(def db-uri "datomic:mem://pedidos") ;; banco em memória para testes

(defn init-db []
  ;; Cria o banco se não existir
  (d/create-database db-uri)
  (let [conn (d/connect db-uri)]
    ;; Transact schema - todas mudanças em Datomic são transações
    @(d/transact conn schema/schema)
    conn))

(defn create-pedido
  "Cria um pedido no Datomic. Usa mapas aninhados para entidades relacionadas"
  [conn pedido-data]
  (let [tx-data [(merge {:db/id "temp-pedido" ;; id temporário para transação
                         :pedido/id (java.util.UUID/randomUUID)}
                        pedido-data)]]
    @(d/transact conn tx-data)))

(defn find-pedido-by-id
  "Query Datalog - linguagem de consulta do Datomic baseada em lógica"
  [db pedido-id]
  ;; :find - o que retornar; :where - condições; ? indica variável lógica
  (d/q '[:find (pull ?e [*]) . ;; pull busca entidade completa
         :in $ ?id
         :where [?e :pedido/id ?id]]
       db
       pedido-id))

(defn find-all-pedidos
  "Retorna todos pedidos com status específico"
  [db status]
  (d/q '[:find [(pull ?e [* {:pedido/itens [*]}]) ...] ;; pull com navegação
         :in $ ?status
         :where [?e :pedido/status ?status]]
       db
       status))

(defn update-pedido-status
  "Atualiza status - em Datomic nunca apagamos, só adicionamos novos fatos"
  [conn pedido-id novo-status]
  (let [db (d/db conn)
        eid (d/q '[:find ?e .
                   :in $ ?id
                   :where [?e :pedido/id ?id]]
                 db
                 pedido-id)]
    @(d/transact conn [{:db/id eid
                        :pedido/status novo-status}])))