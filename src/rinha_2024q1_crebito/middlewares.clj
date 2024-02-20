(ns rinha-2024q1-crebito.middlewares
  (:require [rinha-2024q1-crebito.db :as db]))

(defn wrap-db
  [handler]
  (fn [request]
    (handler (assoc request :db-spec db/spec))))

(defn wrap-clientes
  [handler]
  (fn [request]
    (handler (assoc request :clientes {1 {:id 1 :limite 100000}
                                       2 {:id 2 :limite 80000}
                                       3 {:id 3 :limite 1000000}
                                       4 {:id 4 :limite 10000000}
                                       5 {:id 5 :limite 500000}}))))
