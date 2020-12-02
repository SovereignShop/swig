(ns swig.events.core
  (:require
   [swig.macros #?(:cljs :refer-macros :clj :refer) [def-event-ds]]))

(def-event-ds :swig.events.core/initialize [_ facts] facts)
