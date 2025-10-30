(defproject pedidos-api "0.1.0"
  :description "API de pedidos com Clojure, Datomic e Kafka"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.datomic/datomic-free "0.9.5697"]
                 [org.apache.kafka/kafka-clients "3.5.1"]
                 [cheshire "5.11.0"]
                 [ring/ring-core "1.10.0"]
                 [ring/ring-jetty-adapter "1.10.0"]
                 [ring/ring-json "0.5.1"]
                 [compojure "1.7.0"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.1"]]}}
  :main ^:skip-aot pedidos-api.core
  :target-path "target/%s")