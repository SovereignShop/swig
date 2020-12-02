(ns swig.views.window
  (:require
   [swig.dispatch :as methods]))

(defmethod methods/dispatch :swig.type/window
  ([props]
   (methods/wrap props)))
