(ns swig.events.element
  #?(:cljs
     (:require-macros
      [swig.macros :refer [def-event-ds]]))
  (:require
   #?(:clj [swig.macros :refer [def-event-ds]])))

(def context-ident
  [:swig/ident :swig.ident/context])

(def-event-ds ::set-context
  [_ id]
  [{:db/ident context-ident
    :swig.context/id id}])
