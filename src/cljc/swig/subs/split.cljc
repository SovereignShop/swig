(ns swig.subs.split
  (:require
   [swig.macros :as m]))

(m/def-pull-sub ::get-split
  [:swig.split/split-percent
   :swig.split/ops
   :swig.split/orientation])
