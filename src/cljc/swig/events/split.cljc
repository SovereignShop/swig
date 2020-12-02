(ns swig.events.split
  (:require
   [swig.macros :as m]))

(m/def-event-ds :siwg.events.split/set-split-percent
  [_ split-id percent]
  [[:db/add split-id :swig.split/split-percent (+ (float percent) 0.001)]])
