(ns swig.subs.operations
  (:require
   [swig.macros :as m]))

(m/def-sub ::get-op-names
  '[:find [?name ...]
    :in $ [?id ...]
    :where
    [?id :swig.operation/name ?name]])

(m/def-pull-sub ::get-operation
  [:db/id
   :swig/type])

(m/def-pull-sub ::get-operations
  '[:db/id
    :swig/type
    :swig.operations/ops])

(m/def-sub ::op-get-frame
  '[:find ?frame-id .
    :in $ ?op-id
    :where
    [?op-id :swig.ref/parent ?ops-id]
    [?frame-id :swig.frame/ops ?ops-id]])
