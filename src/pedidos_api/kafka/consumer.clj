(ns pedidos-api.kafka.consumer
  (:require [cheshire.core :as json])
  (:import [org.apache.kafka.clients.consumer KafkaConsumer]
           [org.apache.kafka.common.serialization StringDeserializer]
           [java.time Duration]))

(defn create-consumer [group-id]
  (let [props {"bootstrap.servers" "localhost:9092"
               "group.id" group-id ;; consumer group - permite paralelismo
               "key.deserializer" (.getName StringDeserializer)
               "value.deserializer" (.getName StringDeserializer)
               "auto.offset.reset" "earliest" ;; lê do início se novo consumer
               "enable.auto.commit" "true"}]
    (KafkaConsumer. props)))

(defn consume-messages
  "Consome mensagens de um tópico. handler-fn recebe map com :key e :value"
  [consumer topics handler-fn]
  (.subscribe consumer topics)
  ;; Loop infinito - em produção use atom/promise para controlar parada
  (loop []
    (let [records (.poll consumer (Duration/ofMillis 1000))] ;; poll com timeout
      (doseq [record records]
        (try
          (handler-fn {:key (.key record)
                       :value (json/parse-string (.value record) true)
                       :partition (.partition record)
                       :offset (.offset record)})
          (catch Exception e
            (println "Erro processando mensagem:" (.getMessage e)))))
      (when-not (Thread/interrupted)
        (recur)))))

(defn close-consumer [consumer]
  (.close consumer))