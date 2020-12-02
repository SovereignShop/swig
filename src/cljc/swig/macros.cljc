(ns swig.macros
  (:require
   #?@(:clj [[clojure.core.match :refer [match]]]
       :cljs [[cljs.core.match :refer-macros [match]]
              [re-posh.core :as re-posh]])))

(defmacro match2
  {:style/indent [:defn]}
  [vars & clauses]
  (let [clauses (mapcat (fn [[match-expr consequent]]
                          `[~(eval match-expr) ~consequent])
                        (partition 2 clauses))]
    `(cljs.core.match/match ~vars ~@clauses)))

(defmacro def-event-ds
  [k args & body]
  (let [sym (symbol (name k))]
    `(do (defn ~sym ~args ~@body)
         #?(:cljs
            (re-posh.core/reg-event-ds ~k (fn [db params]
                                            (apply ~sym db (next params))))))))

(defn compile-query [query]
  (match query
         [:find (['pull id pattern] :seq) '. & more]
         [:pull pattern (into [:find id '.] more)]

         [:find (['pull-with op id pattern] :seq) '. & more]
         [:pull-with op pattern (into [:find id '.] more)]

         [:find (['pull id pattern] :seq) & more]
         [:pull-many pattern (into [:find [id '...]] more)]

         :else [:none nil query]))

(defmacro def-sub
  ^{:style/indent [:defn]}
  ([query-name query]
   (let [parsed-query    (compile-query query)
         query-name-sym  (symbol (name query-name))
         signal-fn-name  (symbol (str (name query-name) "-signal"))
         handler-fn-name (symbol (str (name query-name) "-handler"))
         find-ids-query  (keyword (namespace query-name)
                                  (str (gensym (name query-name))))]
     (match parsed-query
            [:none nil find-expr]
            `(do (def ~query-name-sym (quote ~query))
                 (defn ~handler-fn-name [_# variables#]
                   {:type      :query
                    :query     (quote ~query)
                    :variables (next variables#)})
                 (re-posh.core/reg-sub ~query-name ~handler-fn-name))
            [:pull-many pull-pattern find-expr]
            `(do (def ~query-name-sym (quote ~query))
                 (re-posh.core/reg-query-sub ~find-ids-query (quote ~find-expr))
                 (defn ~signal-fn-name  [params#]
                   (re-posh.core/subscribe (into [~find-ids-query] (next params#))) )
                 (defn ~handler-fn-name [ids# _#]
                   {:type    :pull-many
                    :pattern (quote ~pull-pattern)
                    :ids     (flatten ids#)})
                 (re-posh.core/reg-sub ~query-name ~signal-fn-name ~handler-fn-name))

            [:pull pull-pattern find-expr]
            `(do (def ~query-name-sym (quote ~query))
                 (re-posh.core/reg-query-sub ~find-ids-query (quote ~find-expr))
                 (defn ~signal-fn-name  [params#]
                   (re-posh.core/subscribe (into [~find-ids-query] (next params#))) )
                 (defn ~handler-fn-name [ids# _#]
                   {:type    :pull
                    :pattern (quote ~pull-pattern)
                    :id      ids#})
                 (re-posh.core/reg-sub ~query-name ~signal-fn-name ~handler-fn-name))

            [:pull-with op pull-pattern find-expr]
            `(do (def ~query-name-sym (quote ~query))
                 (re-posh.core/reg-query-sub ~find-ids-query (quote ~find-expr))
                 (defn ~signal-fn-name  [params#]
                   (re-posh.core/subscribe (into [~find-ids-query] (next params#))))
                 (defn ~handler-fn-name [ids# _#]
                   {:type    :pull-many
                    :pattern (quote ~pull-pattern)
                    :ids     (~op ids#)})
                 (re-posh.core/reg-sub ~query-name ~signal-fn-name ~handler-fn-name))))))

(defmacro def-pull-sub
  ^{:style/indent [:defn]}
  [query-name pattern]
  `(re-posh.core/reg-pull-sub ~query-name (quote ~pattern)))

(defmacro def-pull-many-sub
  ^{:style/indent [:defn]}
  [query-name pattern]
  `(re-posh.core/reg-pull-many-sub ~query-name (quote ~pattern)))

