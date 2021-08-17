(ns swig.subs.view
  (:require
   [re-posh.core :as re-posh]
   [swig.macros :as m]))


(m/def-sub :swig.subs.view/get-active-tab
  [:find (pull ?tab-id [:swig.tab/fullscreen
                        :swig/index
                        :swig.tab/handler
                        :swig.tab/label
                        :swig.tab/order
                        :swig.element/ops
                        :swig/type
                        :swig/ident
                        :db/id]) .
   :in $ ?view-id
   :where
   [?view-id :swig.view/active-tab ?tab-id]])

(m/def-sub :swig.subs.view/get-tabs
  [:find (pull ?tab-id [:swig.tab/fullscreen
                        :swig/index
                        :swig.tab/handler
                        {:swig.tab/label
                         [:swig/type
                          :swig.cell/element]}
                        :swig.tab/order
                        {:swig.element/ops
                         [:swig/type
                          {:swig.operations/ops
                           [:swig/type]}]}
                        {:swig.container/capabilities []}
                        :swig/ident
                        :db/id
                        :swig.dispatch/handler
                        :swig/type])
   :in $ ?view-id
   :where
   [?view-id :swig.ref/child ?tab-id]
   [?tab-id :swig/type :swig.type/tab]])

(m/def-pull-sub :swig.subs.view/get-view-ops
  [:swig.element/ops
   :swig.view/tab-type])

(m/def-sub :swig.subs.view/get-view-ids
  [:find [?view-id ...]
   :in $ ?split-id
   :where
   [?split-id :swig.ref/child ?view-id]])
