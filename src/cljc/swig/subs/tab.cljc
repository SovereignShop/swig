(ns swig.subs.tab
  (:require
   [swig.macros :as m]))

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

(m/def-pull-sub ::get-tab
  [:swig.tab/fullscreen
   :swig.tab/handler
   :swig.tab/label
   :swig.tab/order
   :swig.tab/ops
   :swig/type
   :swig.ref/parent
   :swig/ident
   :db/id])
