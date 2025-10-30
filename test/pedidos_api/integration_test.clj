(ns pedidos-api.integration-test
  (:require [clojure.test :refer :all]
            [pedidos-api.datomic.db :as datomic]
            [pedidos-api.kafka.producer :as kafka-prod]
            [pedidos-api.service :as service]
            [datomic.api :as d]))

;; Testes de integração simplificados - testam Datomic e Kafka Producer
(deftest test-processar-pedido-completo
  (testing "Fluxo completo de criação de pedido"
    (let [conn (datomic/init-db)
          producer (kafka-prod/create-producer)]
      
      ;; Processa pedido
      (let [resultado (service/processar-pedido
                       conn
                       producer
                       "joao@email.com"
                       [{:produto "Notebook"
                         :quantidade 1
                         :preco-unitario 3500.00}])]
        
        ;; Verifica resultado
        (is (:success resultado))
        (is (some? (:pedido-id resultado)))
        (is (= 3500.0 (:total resultado)))
        
        ;; Verifica Datomic
        (let [db (d/db conn)
              pedido (datomic/find-pedido-by-id db (java.util.UUID/fromString (:pedido-id resultado)))]
          (is (some? pedido))
          (is (= "joao@email.com" (:pedido/cliente-email pedido)))
          (is (= :pendente (:pedido/status pedido)))
          (is (= 1 (count (:pedido/itens pedido))))
          (is (= "Notebook" (:item/produto-nome (first (:pedido/itens pedido)))))))
      
      ;; Cleanup
      (kafka-prod/close-producer producer))))

(deftest test-aprovar-pedido-integrado
  (testing "Aprovação de pedido atualiza Datomic e publica no Kafka"
    (let [conn (datomic/init-db)
          producer (kafka-prod/create-producer)]
      
      ;; Cria pedido primeiro
      (let [resultado (service/processar-pedido
                       conn
                       producer
                       "maria@email.com"
                       [{:produto "Mouse" :quantidade 2 :preco-unitario 120.00}])
            pedido-id (java.util.UUID/fromString (:pedido-id resultado))]
        
        ;; Aprova pedido
        (service/aprovar-pedido conn producer pedido-id)
        
        ;; Verifica Datomic
        (let [db (d/db conn)
              pedido (datomic/find-pedido-by-id db pedido-id)]
          (is (= :aprovado (:pedido/status pedido)))))
      
      (kafka-prod/close-producer producer))))

(deftest test-listar-pedidos-por-status
  (testing "Busca múltiplos pedidos por status"
    (let [conn (datomic/init-db)
          producer (kafka-prod/create-producer)]
      
      ;; Cria 2 pedidos pendentes
      (service/processar-pedido conn producer "user1@email.com"
                                [{:produto "Item1" :quantidade 1 :preco-unitario 100}])
      (service/processar-pedido conn producer "user2@email.com"
                                [{:produto "Item2" :quantidade 1 :preco-unitario 200}])
      
      ;; Cria e aprova 1 pedido
      (let [resultado (service/processar-pedido conn producer "user3@email.com"
                                                [{:produto "Item3" :quantidade 1 :preco-unitario 300}])
            pedido-id (java.util.UUID/fromString (:pedido-id resultado))]
        (service/aprovar-pedido conn producer pedido-id))
      
      ;; Verifica contagens
      (let [db (d/db conn)
            pendentes (datomic/find-all-pedidos db :pendente)
            aprovados (datomic/find-all-pedidos db :aprovado)]
        (is (= 2 (count pendentes)))
        (is (= 1 (count aprovados))))
      
      (kafka-prod/close-producer producer))))