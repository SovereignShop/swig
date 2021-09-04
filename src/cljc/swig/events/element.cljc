(ns swig.events.element
  #?(:cljs
     (:require-macros
      [swig.macros :refer [def-event-ds]]))
  (:require
   [swig.core :refer [context-ident]]
   #?(:clj [swig.macros :refer [def-event-ds]])))

(def-event-ds ::set-context
  [db id]
  [[:db/add context-ident :swig.context/id id]])
