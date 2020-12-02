(ns swig.subs.view
  (:require
   [swig.macros :as m]))

(m/def-sub :swig.subs.view/get-active-tab
  '[:find (pull ?tab-id [:swig.tab/fullscreen
                         :swig.tab/handler
                         :swig.tab/label
                         :swig.tab/order
                         :swig.tab/ops
                         :swig/type
                         :swig.ref/parent
                         :swig/ident
                         :db/id]) .
    :in $ ?view-id
    :where
    [?view-id :swig.view/active-tab ?tab-id]])

(m/def-sub ::get-tabs
  '[:find (pull ?tab-id [:swig.tab/fullscreen
                         :swig.tab/handler
                         :swig.ref/parent
                         {:swig.tab/label
                          [:swig/type
                           :swig.cell/element]}
                         :swig.tab/order
                         {:swig.tab/ops
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
    [?tab-id :swig.ref/parent ?view-id]
    [?tab-id :swig/type :swig.type/tab]])

(m/def-pull-sub :swig.subs.view/get-view-ops
  [:swig.view/ops
   :swig.view/tab-type])

(m/def-sub :swig.subs.view/get-view-ids
  '[:find [?view-id ...]
    :in $ ?split-id
    :where
    [?view-id :swig.ref/parent ?split-id]])
