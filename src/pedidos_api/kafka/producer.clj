(ns pedidos-api.kafka.producer
  (:require [cheshire.core :as json])
  (:import [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]
           [org.apache.kafka.common.serialization StringSerializer]))

;; Configuração do producer Kafka
(defn create-producer []
  (let [props {"bootstrap.servers" "localhost:9092" ;; endereço do broker Kafka
               "key.serializer" (.getName StringSerializer)
               "value.serializer" (.getName StringSerializer)
               "acks" "all"}] ;; aguarda confirmação de todos os brokers
    (KafkaProducer. props)))

(defn send-message
  "Envia mensagem para tópico Kafka de forma assíncrona"
  [producer topic key value]
  (let [record (ProducerRecord. topic key (json/generate-string value))]
    ;; send retorna Future - aqui usamos callback
    (.send producer record
           (reify org.apache.kafka.clients.producer.Callback
             (onCompletion [_ metadata exception]
               (if exception
                 (println "Erro ao enviar:" (.getMessage exception))
                 (println "Enviado para partition" (.partition metadata)
                          "offset" (.offset metadata))))))))

(defn close-producer [producer]
  (.close producer))