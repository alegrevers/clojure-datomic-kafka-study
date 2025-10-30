(ns pedidos-api.service
  (:require [pedidos-api.datomic.db :as datomic]
            [pedidos-api.kafka.producer :as kafka-prod]
            [datomic.api :as d]))

;; Service layer - orquestra Datomic e Kafka
(defn processar-pedido
  "Fluxo: salva no Datomic e publica no Kafka"
  [conn producer email itens]
  (try
    ;; Calcula total e cria pedido no Datomic
    (let [total (reduce + (map #(* (:quantidade %) (:preco-unitario %)) itens))
          pedido-id (java.util.UUID/randomUUID)
          ;; Itens precisam de :db/id temporário para entidades aninhadas
          itens-com-id (mapv #(merge {:db/id (str "item-" (java.util.UUID/randomUUID))}
                                      {:item/produto-nome (:produto %)
                                       :item/quantidade (:quantidade %)
                                       :item/preco-unitario (bigdec (:preco-unitario %))})
                             itens)
          pedido-data {:pedido/id pedido-id
                       :pedido/cliente-email email
                       :pedido/total (bigdec total)
                       :pedido/status :pendente
                       :pedido/itens itens-com-id}]
      (datomic/create-pedido conn pedido-data)
      
      ;; Publica evento no Kafka
      (kafka-prod/send-message producer "pedidos-eventos"
                               (str pedido-id)
                               {:evento "pedido_criado"
                                :pedido_id (str pedido-id)
                                :email email
                                :total total})
      
      {:success true :pedido-id (str pedido-id) :total total})
    (catch Exception e
      {:success false :error (.getMessage e)})))

(defn aprovar-pedido
  "Atualiza status do pedido"
  [conn producer pedido-id]
  (datomic/update-pedido-status conn pedido-id :aprovado)
  (kafka-prod/send-message producer "pedidos-eventos"
                           (str pedido-id)
                           {:evento "pedido_aprovado"
                            :pedido_id (str pedido-id)}))