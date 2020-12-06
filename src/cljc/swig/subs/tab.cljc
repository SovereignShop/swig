(ns swig.subs.tab
  (:require
   [swig.macros :as m]))

(m/def-pull-sub ::get-tab
  [:swig.tab/fullscreen
   :swig.tab/handler
   :swig.tab/label
   :swig.tab/order
   :swig.tab/ops
   :swig/index
   :swig/type
   :swig.ref/parent
   :swig/ident
   :db/id])
