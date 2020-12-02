(ns swig.subs.split
  (:require
   [swig.macros :as m]))

(m/def-pull-sub :swig.subs.split/get-split
  [:swig.split/split-percent
   :swig.split/ops
   :swig.split/orientation])
