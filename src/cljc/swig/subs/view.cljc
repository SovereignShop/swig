(ns swig.subs.view
  (:require
   [swig.macros :as m]))

(m/def-sub ::get-active-tab
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

(m/def-pull-sub ::get-view-ops
  [:swig.view/ops
   :swig.view/tab-type])

(m/def-sub ::get-view-ids
  '[:find [?view-id ...]
    :in $ ?split-id
    :where
    [?view-id :swig.ref/parent ?split-id]])
