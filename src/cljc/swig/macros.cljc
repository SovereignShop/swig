(ns swig.macros
  (:require
   #?@(:clj [[clojure.core.match :refer [match]]]
       :cljs [[cljs.core.match :refer-macros [match]]
              [re-posh.core :as re-posh]])))

(defn disect-test-expr [expr]
  (if (list? expr)
    (let [evals
          (for [e expr
                :when (list? e)]
            e)]
      )
    expr))

(defmacro dcond [& exprs]
  (let [new-exprs (apply concat
                         (for [[test-expr cons-expr] (partition 2 exprs)]
                           `(~test-expr (do
                                          (let [x# (swap! skyhook.interpreter/cnt inc)
                                                ret# ~cons-expr]
                                            (println (str x# ":" (quote ~test-expr) ":\n") ret#)
                                            ret#)))))]
    `(do
       (reset! skyhook.interpreter/cnt 0)
       (cond ~@new-exprs))))

(defmacro match2
  {:style/indent [:defn]}
  [vars & clauses]
  (let [clauses (mapcat (fn [[match-expr consequent]]
                          `[~(eval match-expr) ~consequent])
                        (partition 2 clauses))]
    `(cljs.core.match/match ~vars ~@clauses)))

(defn if-cljs [env consequent alternate]
  (if (:ns env)
    consequent
    alternate))

(defn when-cljs [env consequent]
  (when (:ns env)
    consequent))

(defmacro def-event-ds
  [k args & body]
  (let [sym (symbol (name k))]
    (if-cljs &env
      `(do (defn ~sym ~args ~@body)
           (re-posh.core/reg-event-ds ~k (fn [db# params#]
                                           (apply ~sym db# (next params#)))))
      `(defn ~sym ~args ~@body))))

(defn compile-query [query]
  (match query
         [:find (['pull id pattern] :seq) '. & more]
         [:pull pattern (into [:find id '.] more)]

         [:find (['pull-entity id pattern] :seq) '. & more]
         [:pull-entity pattern (into [:find id '.] more)]

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
            (if-cljs &env
              `(do (def ~query-name-sym (quote ~query))
                   (defn ~handler-fn-name [_# variables#]
                     {:type      :query
                      :query     (quote ~query)
                      :variables (next variables#)})
                   (re-posh.core/reg-sub ~query-name ~handler-fn-name))
              `(def ~query-name-sym (quote ~query)))
            [:pull-many pull-pattern find-expr]
            (if-cljs &env
              `(do (def ~query-name-sym (quote ~query))
                   (re-posh.core/reg-query-sub ~find-ids-query (quote ~find-expr))
                   (defn ~signal-fn-name  [params#]
                     (re-posh.core/subscribe (into [~find-ids-query] (next params#))) )
                   (defn ~handler-fn-name [ids# _#]
                     {:type    :pull-many
                      :pattern (quote ~pull-pattern)
                      :ids     (flatten ids#)})
                   (re-posh.core/reg-sub ~query-name ~signal-fn-name ~handler-fn-name))
              `(def ~query-name-sym (quote ~query)))

            [:pull pull-pattern find-expr]
            (if-cljs &env
              `(do (def ~query-name-sym (quote ~query))
                   (re-posh.core/reg-query-sub ~find-ids-query (quote ~find-expr))
                   (defn ~signal-fn-name  [params#]
                     (re-posh.core/subscribe (into [~find-ids-query] (next params#))) )
                   (defn ~handler-fn-name [ids# _#]
                     {:type    :pull
                      :pattern (quote ~pull-pattern)
                      :id      ids#})
                   (re-posh.core/reg-sub ~query-name ~signal-fn-name ~handler-fn-name))
              `(def ~query-name-sym (quote ~query)))

            [:pull-entity pull-pattern find-expr]
            (if-cljs &env
              `(do (def ~query-name-sym (quote ~query))
                   (re-posh.core/reg-query-sub ~find-ids-query (quote ~find-expr))
                   (defn ~signal-fn-name  [params#]
                     (re-posh.core/subscribe (into [~find-ids-query] (next params#))) )
                   (defn ~handler-fn-name [ids# _#]
                     {:type    :pull-entity
                      :pattern (quote ~pull-pattern)
                      :id      ids#})
                   (re-posh.core/reg-sub ~query-name ~signal-fn-name ~handler-fn-name))
              `(def ~query-name-sym (quote ~query)))

            [:pull-with op pull-pattern find-expr]
            (if-cljs &env
              `(do (def ~query-name-sym (quote ~query))
                   (re-posh.core/reg-query-sub ~find-ids-query (quote ~find-expr))
                   (defn ~signal-fn-name  [params#]
                     (re-posh.core/subscribe (into [~find-ids-query] (next params#))))
                   (defn ~handler-fn-name [ids# _#]
                     {:type    :pull-many
                      :pattern (quote ~pull-pattern)
                      :ids     (~op ids#)})
                   (re-posh.core/reg-sub ~query-name ~signal-fn-name ~handler-fn-name))
              `(def ~query-name-sym (quote ~query)))))))

(defmacro def-pull-sub
  ^{:style/indent [:defn]}
  [query-name pattern]
  (when-cljs &env
    `(re-posh.core/reg-pull-sub ~query-name (quote ~pattern))))

(defmacro def-pull-many-sub
  ^{:style/indent [:defn]}
  [query-name pattern]
  (when-cljs &env
    `(re-posh.core/reg-pull-many-sub ~query-name (quote ~pattern))))

(defmacro set-attr! [db entity attr value]
  `(let [ent# (datascript.core/entity ~db ~entity)]
     (when-let [obj# (:three/obj ent#)]
       (oops.core/oset! obj# ~attr ~value))))
