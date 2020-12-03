(ns swig.subs.resize
  (:require
   [swig.macros :as m]))

(m/def-sub :swig.subs.resize/resize-frame-id
  [:find ?id .
   :in $ ?container-id
   :where
   [?container-id :swig.capability.resize/frame-id ?id]])

(m/def-pull-sub :swig.subs.resize/resize-start-pose
  [:swig.capability.resize/start-left
   :swig.capability.resize/start-top
   :swig.frame/left
   :swig.frame/top])
