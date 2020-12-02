(ns swig.events.split
  (:require
   [swig.macros :as m]))

(m/def-event-ds ::set-split-percent
  [_ split-id percent]
  [[:db/add split-id :swig.split/split-percent (+ (float percent) 0.001)]])
