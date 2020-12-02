(ns swig.events.core
  (:require
   [swig.macros :as m]))

(m/def-event-ds ::initialize [_ facts] facts)
