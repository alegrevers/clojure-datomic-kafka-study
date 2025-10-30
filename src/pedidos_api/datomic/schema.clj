(ns pedidos-api.datomic.schema)

;; Schema Datomic - definição de entidades e atributos
;; Datomic usa triplas [entidade atributo valor] chamadas datoms
(def schema
  [;; Pedido
   {:db/ident       :pedido/id
    :db/valueType   :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity ;; identifica unicamente o pedido
    :db/doc         "ID único do pedido"}
   
   {:db/ident       :pedido/cliente-email
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Email do cliente"}
   
   {:db/ident       :pedido/itens
    :db/valueType   :db.type/ref ;; referência a outra entidade
    :db/cardinality :db.cardinality/many ;; um pedido tem vários itens
    :db/doc         "Itens do pedido"}
   
   {:db/ident       :pedido/total
    :db/valueType   :db.type/bigdec
    :db/cardinality :db.cardinality/one
    :db/doc         "Valor total do pedido"}
   
   {:db/ident       :pedido/status
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc         "Status: :pendente, :aprovado, :enviado"}
   
   ;; Item de Pedido
   {:db/ident       :item/produto-nome
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   
   {:db/ident       :item/quantidade
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}
   
   {:db/ident       :item/preco-unitario
    :db/valueType   :db.type/bigdec
    :db/cardinality :db.cardinality/one}])