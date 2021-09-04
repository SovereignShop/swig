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
   [?parent-id :swig.ref/child ?id]])

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

(m/def-pull-sub :swig.subs.element/get-props
  [{:swig.element/ops
    [:swig/type
     {:swig.operations/ops
      [:swig/type]}]}
   :swig.container/capabilities
   :swig.element/maximized-element
   :swig/ident
   :swig/index
   :swig/type
   :swig.ref/child])
