(ns swig.subs.tab
  (:require
   [swig.macros :as m]))

(m/def-pull-sub ::get-tab
  [:swig.tab/fullscreen
   :swig.tab/handler
   :swig.tab/label
   :swig.tab/order
   :swig.element/ops
   :swig/index
   :swig/type
   :swig/ident
   :db/id])
