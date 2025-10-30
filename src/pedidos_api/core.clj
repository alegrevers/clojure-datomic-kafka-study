(ns pedidos-api.core
  (:require [pedidos-api.datomic.db :as datomic]
            [pedidos-api.kafka.producer :as kafka-prod]
            [pedidos-api.service :as service]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes GET POST PUT]]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [datomic.api :as d])
  (:gen-class))

;; Estado global (em produção, use component/mount)
(defonce state (atom {:conn nil :producer nil}))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body data})

;; Rotas HTTP
(defroutes app-routes
  ;; Criar pedido
  (POST "/pedidos" {body :body}
    (let [result (service/processar-pedido
                  (:conn @state)
                  (:producer @state)
                  (:email body)
                  (:itens body))]
      (if (:success result)
        (json-response result 201)
        (json-response result 400))))
  
  ;; Aprovar pedido
  (PUT "/pedidos/:id/aprovar" [id]
    (try
      (service/aprovar-pedido (:conn @state)
                              (:producer @state)
                              (java.util.UUID/fromString id))
      (json-response {:success true :message "Pedido aprovado"})
      (catch Exception e
        (json-response {:success false :error (.getMessage e)} 400))))
  
  ;; Buscar pedido por ID
  (GET "/pedidos/:id" [id]
    (try
      (let [pedido (datomic/find-pedido-by-id
                    (d/db (:conn @state))
                    (java.util.UUID/fromString id))]
        (if pedido
          (json-response pedido)
          (json-response {:error "Pedido não encontrado"} 404)))
      (catch Exception e
        (json-response {:error (.getMessage e)} 400))))
  
  ;; Listar pedidos por status
  (GET "/pedidos" {params :params}
    (let [status (keyword (or (:status params) "pendente"))
          pedidos (datomic/find-all-pedidos (d/db (:conn @state)) status)]
      (json-response {:pedidos pedidos :total (count pedidos)})))
  
  ;; Health check
  (GET "/health" []
    (json-response {:status "ok" :kafka "connected" :datomic "connected"}))
  
  (route/not-found (json-response {:error "Rota não encontrada"} 404)))

(def app
  (-> app-routes
      wrap-keyword-params
      wrap-params
      (wrap-json-body {:keywords? true})
      wrap-json-response))

(defn -main [& args]
  (let [conn (datomic/init-db)
        producer (kafka-prod/create-producer)]
    (swap! state assoc :conn conn :producer producer)
    (println "===========================================")
    (println "Servidor iniciado na porta 3000")
    (println "Teste: http://localhost:3000/health")
    (println "===========================================")
    (jetty/run-jetty app {:port 3000 :join? false})))