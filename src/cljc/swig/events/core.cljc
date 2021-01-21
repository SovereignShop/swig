(ns swig.events.core
  (:require
   [swig.macros :refer-macros [def-event-ds]]))

(def-event-ds :swig.events.core/initialize [_ facts] facts)
