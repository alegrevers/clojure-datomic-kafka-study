(ns pedidos-api.integration-test
  (:require [clojure.test :refer :all]
            [pedidos-api.datomic.db :as datomic]
            [pedidos-api.kafka.producer :as kafka-prod]
            [pedidos-api.kafka.consumer :as kafka-cons]
            [pedidos-api.service :as service]
            [datomic.api :as d])
  (:import [java.util.concurrent CountDownLatch TimeUnit]))

;; Testes de integração - testam interação entre Datomic e Kafka
(deftest test-processar-pedido-completo
  (testing "Fluxo completo de criação de pedido"
    (let [conn (datomic/init-db)
          producer (kafka-prod/create-producer)
          latch (CountDownLatch. 1) ;; sincroniza async Kafka
          mensagens (atom [])
          
          ;; Consumer para verificar mensagem Kafka
          consumer (kafka-cons/create-consumer "test-group-1")
          consumer-thread (future
                           (try
                             (kafka-cons/consume-messages
                              consumer
                              ["pedidos-eventos"]
                              (fn [msg]
                                (swap! mensagens conj msg)
                                (.countDown latch)))
                             (catch Exception e
                               (println "Consumer error:" (.getMessage e)))))]
      
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
        
        ;; Verifica Datomic
        (let [db (d/db conn)
              pedido (datomic/find-pedido-by-id db (java.util.UUID/fromString (:pedido-id resultado)))]
          (is (some? pedido))
          (is (= "joao@email.com" (:pedido/cliente-email pedido)))
          (is (= :pendente (:pedido/status pedido))))
        
        ;; Aguarda mensagem Kafka (timeout 5s)
        (.await latch 5 TimeUnit/SECONDS)
        (is (= 1 (count @mensagens)))
        (is (= "pedido_criado" (get-in @mensagens [0 :value :evento]))))
      
      ;; Cleanup
      (future-cancel consumer-thread)
      (kafka-cons/close-consumer consumer)
      (kafka-prod/close-producer producer))))

(deftest test-aprovar-pedido-integrado
  (testing "Aprovação de pedido atualiza Datomic e publica no Kafka"
    (let [conn (datomic/init-db)
          producer (kafka-prod/create-producer)
          
          ;; Cria pedido primeiro
          resultado (service/processar-pedido
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
        (is (= :aprovado (:pedido/status pedido))))
      
      (kafka-prod/close-producer producer))))