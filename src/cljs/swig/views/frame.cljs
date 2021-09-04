(ns swig.views.frame
  (:require
   [swig.views.element :refer [element]]
   [swig.dispatch :as methods]
   [re-posh.core :as re-posh]
   [re-com.core :as re]))

(defn frame-children [props]
  (let [children (:swig.ref/child props)]
    [re/v-box
     :style {:flex "1 1 0%"}
     :children (mapv element children)]))

(defmethod methods/dispatch :swig.type/frame
  ([{:keys [:db/id] :as props}]
   (let [{:keys [:swig.frame/height
                 :swig.frame/width
                 :swig.frame/left
                 :swig.frame/top
                 :swig.frame/ops]}
         @(re-posh/subscribe [:swig.subs.frame/get-frame id])]
     [re/v-box
      :attr {:id (str "frame-" id)}
      :style {:position :absolute
              :transition "width 0.3s 0.005s, height 0.3s 0.005s, top 0.1s 0.005s, left 0.1s 0.005s, transform 0.5s"
              :border-style "solid"
              :flex "1 1 0%"
              :box-shadow "5px 5px"
              :width width
              :height height
              :top top
              :left left}
      :children (conj (mapv methods/dispatch (:swig.operations/ops ops))
                      [frame-children props])])))
