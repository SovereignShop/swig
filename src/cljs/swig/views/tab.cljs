(ns ^:dev/always swig.views.tab
  (:require
   [swig.views.element :refer [element]]
   [swig.dispatch :as methods]
   [re-posh.core :as re-posh]
   [re-com.core :as re]))

(defmethod methods/dispatch :swig.type/tab
  ([{:keys [:db/id] :as props}]
   (let [child        (first (:swig.ref/child props))
         ops          (:swig.element/ops props)
         container-id (str "tab-" id)]
     (println "children:" (:swig.ref/child props))
     [re/h-box
      :attr {:id container-id}
      :class "swig-tab"
      :style {:flex "1 1 0%"}
      :children
      [(when ops
         (methods/dispatch ops))
       (when child
         (println "element:" child)
         [element child])]])))
