(ns swig.events.core
  (:require
   [swig.macros :as m]))

(m/def-event-ds :swig.events.core/initialize [_ facts] facts)
