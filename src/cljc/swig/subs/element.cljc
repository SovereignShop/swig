(ns swig.subs.element
  (:require
   [swig.macros :as m]))

(m/def-pull-sub :swig.subs.element/get-element
  [:db/id
   :swig/ident
   :swig/index
   :swig.dispatch/handler
   :swig.container/capabilities
   :swig/type])

(m/def-sub :swig.subs.element/get-parent
  [:find ?parent-id .
    :in $ ?id
    :where
    [?id :swig.ref/parent ?parent-id]])

(m/def-sub ::get-handler
  [:find ?handler .
   :in $ ?view-id
   :where
   [?view-id :swig.dispatch/handler ?handler]])

(m/def-sub :swig.subs.element/get-type
  [:find ?type .
   :in $ ?id
   :where
   [?id :swig/type ?type]])

(m/def-sub :swig.subs.element/get-children
  [:find (pull ?child-id [:db/id
                          {:swig.tab/ops
                           [:swig/type
                            {:swig.operations/ops
                             [:swig/type]}]}
                          :swig.container/capabilities
                          :swig/ident
                          :swig/index
                          :swig.dispatch/handler
                          :swig/type])
   :in $ ?id [?type ...]
   :where
   [?child-id :swig.ref/parent ?id]
   [?child-id :swig/type ?type]])
