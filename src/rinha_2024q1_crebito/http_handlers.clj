(ns rinha-2024q1-crebito.http-handlers
  (:require [next.jdbc :as jdbc]
            [rinha-2024q1-crebito.payloads :as payloads]
            [schema.core :as s]))

(defn ^:private extrato!*
  [{db-spec :db-spec
    cliente_id :cliente-id
    clientes :clientes}]
  (let [resultado (jdbc/execute!
                   db-spec
                   ["(select valor, 'saldo' as tipo, 'saldo' as descricao, now() as realizada_em
                     from saldos
                     where cliente_id = ?)
                     union all
                     (select valor, tipo, descricao, realizada_em
                     from transacoes
                     where cliente_id = ?
                     order by id desc limit 10)"
                    cliente_id cliente_id])
        saldo-row (first resultado)
        saldo {:total        (:valor saldo-row)
               :data_extrato (:realizada_em saldo-row)
               :limite       (:limite (get clientes cliente_id))}
        transacoes (rest resultado)]
    {:status 200
     :body   {:saldo              saldo
              :ultimas_transacoes transacoes}}))

(defn ^:private transacionar!*
  [{db-spec :db-spec
    payload :body
    cliente_id :cliente-id
    clientes :clientes}]
  (if-not (s/check payloads/Transacao payload)
    (let [{limite :limite} (get clientes cliente_id)
          {valor     :valor
           tipo      :tipo
           descricao :descricao} payload
          proc {"d" "debitar"
                "c" "creditar"}
          {novo-saldo   :novo_saldo
           possui-erro? :possui_erro
           mensagem     :mensagem} (jdbc/execute-one!
                                    db-spec
                                    [(format "select novo_saldo, possui_erro, mensagem from %s(?, ?, ?)" (proc tipo))
                                     cliente_id
                                     valor
                                     descricao])]
      (if possui-erro?
        {:status 422
         :body {:erro mensagem}}
        {:status 200
         :body {:limite limite
                :saldo novo-saldo}}))
    {:status 422
     :body   {:erro "manda essa merda direito com 'valor', 'tipo' e 'descricao'"}}))

(defn find-cliente-handler-wrapper
  [handler]
  (fn [{{cliente_id* :id} :route-params
        clientes :clientes :as request}]
    (if-let [{cliente_id :id} (get clientes (Integer/parseInt cliente_id*))]
      (handler (assoc request :cliente-id (int cliente_id)))
      {:status 404})))

(def transacionar! (find-cliente-handler-wrapper transacionar!*))

(def extrato! (find-cliente-handler-wrapper extrato!*))

(defn admin-reset-db!
  [{:keys [db-spec]}]
  (jdbc/execute-one! db-spec
                     ["update saldos set valor = 0;
                       truncate table transacoes;"])
  {:status 200
   :body {:msg "db reset!"}})
