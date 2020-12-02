(ns swig.subs.frame
  (:require
   [swig.macros :as m]))

(m/def-pull-sub :swig.subs.frame/get-frame
  [:swig.frame/left
   :swig.frame/top
   :swig.frame/width
   :swig.frame/height
   {:swig.frame/ops
    [:swig/type
     {:swig.operations/ops
      [:swig/type]}]}])
