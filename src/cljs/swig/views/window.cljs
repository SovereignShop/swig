(ns swig.views.window
  (:require
   [swig.methods :as methods]))

(defmethod methods/dispatch :swig.type/window
  ([props] (methods/wrap props))
  ([props child] (methods/wrap props child)))
