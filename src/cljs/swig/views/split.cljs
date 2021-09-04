(ns swig.views.split
  (:require
   [swig.views.element :refer [element]]
   [swig.dispatch :as methods]
   [re-posh.core :as re-posh]
   [clojure.string :as str]
   [re-com.core :as re]))

(defn maps [f coll]
  (into #{} (map f) coll))

(defmethod methods/dispatch :swig.type/split
  [props]
  (let [split-id (:db/id props)
        children (:swig.ref/child props)
        split    @(re-posh/subscribe [:swig.subs.split/get-split split-id])
        ops      (->> split :swig.element/ops :swig.operations/ops (maps :swig/type))]
    (println "rendering split!")
    [(case (:swig.split/orientation split) :horizontal re/h-split :vertical re/v-split)
     :on-split-change #(re-posh/dispatch [:swig.events.split/set-split-percent (:db/id split) %])
     :style           {:flex   "1 1 0%"
                       :margin "0px"}
     :class "swig-split"
     :initial-split   (:swig.split/split-percent split)
     :panel-1 [element (first children)]
     :panel-2 [element (second children)]]))
