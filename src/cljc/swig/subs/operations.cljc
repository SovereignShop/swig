(ns swig.subs.operations
  (:require
   [swig.macros :as m]))

(m/def-sub :swig.subs.operations/get-op-names
  [:find [?name ...]
    :in $ [?id ...]
    :where
    [?id :swig.operation/name ?name]])

(m/def-pull-sub :swig.subs.operations/get-operation
  [:db/id
   :swig/type])

(m/def-pull-sub ::get-operations
  [:db/id
    :swig/type
    :swig.operations/ops])

(m/def-sub :swig.subs.operations/op-get-frame
  [:find ?frame-id .
   :in $ ?op-id
   :where
   [?ops-id :swig.ref/child ?op-id]
   [?frame-id :swig.frame/ops ?ops-id]])
