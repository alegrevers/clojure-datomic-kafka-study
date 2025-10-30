(ns pedidos-api.unit-test
  (:require [clojure.test :refer :all]
            [pedidos-api.datomic.db :as datomic]
            [datomic.api :as d]))

;; Testes unitários - focam em funções isoladas
(deftest test-datomic-create-pedido
  (testing "Criação de pedido no Datomic"
    (let [conn (datomic/init-db)
          pedido-id (java.util.UUID/randomUUID)
          pedido-data {:pedido/id pedido-id
                       :pedido/cliente-email "teste@email.com"
                       :pedido/total 100M
                       :pedido/status :pendente
                       :pedido/itens [{:item/produto-nome "Produto Teste"
                                       :item/quantidade 1
                                       :item/preco-unitario 100M}]}]
      
      ;; Cria pedido
      (datomic/create-pedido conn pedido-data)
      
      ;; Verifica se foi criado
      (let [db (d/db conn)
            resultado (datomic/find-pedido-by-id db pedido-id)]
        (is (some? resultado) "Pedido deve existir")
        (is (= "teste@email.com" (:pedido/cliente-email resultado)))
        (is (= :pendente (:pedido/status resultado)))
        (is (= 1 (count (:pedido/itens resultado))))))))

(deftest test-datomic-update-status
  (testing "Atualização de status do pedido"
    (let [conn (datomic/init-db)
          pedido-id (java.util.UUID/randomUUID)
          pedido-data {:pedido/id pedido-id
                       :pedido/cliente-email "teste@email.com"
                       :pedido/total 100M
                       :pedido/status :pendente}]
      
      (datomic/create-pedido conn pedido-data)
      (datomic/update-pedido-status conn pedido-id :aprovado)
      
      (let [db (d/db conn)
            resultado (datomic/find-pedido-by-id db pedido-id)]
        (is (= :aprovado (:pedido/status resultado)))))))

(deftest test-datomic-find-by-status
  (testing "Busca pedidos por status"
    (let [conn (datomic/init-db)
          id1 (java.util.UUID/randomUUID)
          id2 (java.util.UUID/randomUUID)]
      
      (datomic/create-pedido conn {:pedido/id id1
                                    :pedido/cliente-email "teste1@email.com"
                                    :pedido/total 100M
                                    :pedido/status :pendente})
      
      (datomic/create-pedido conn {:pedido/id id2
                                    :pedido/cliente-email "teste2@email.com"
                                    :pedido/total 200M
                                    :pedido/status :aprovado})
      
      (let [db (d/db conn)
            pendentes (datomic/find-all-pedidos db :pendente)
            aprovados (datomic/find-all-pedidos db :aprovado)]
        (is (= 1 (count pendentes)))
        (is (= 1 (count aprovados)))
        (is (= id1 (:pedido/id (first pendentes))))))))

(deftest test-calculos-pedido
  (testing "Cálculo de total do pedido"
    (let [itens [{:quantidade 2 :preco-unitario 50}
                 {:quantidade 1 :preco-unitario 100}]
          total (reduce + (map #(* (:quantidade %) (:preco-unitario %)) itens))]
      (is (= 200 total)))))