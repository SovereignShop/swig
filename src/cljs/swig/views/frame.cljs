(ns swig.views.frame
  (:require
   [swig.dispatch :as methods]
   [re-posh.core :as re-posh]
   [re-com.core :as re]))

(defmethod methods/dispatch :swig.type/frame
  ([{:keys [:db/id] :as props}]
   (let [{:keys [:swig.frame/height
                 :swig.frame/width
                 :swig.frame/left
                 :swig.frame/top
                 :swig.frame/ops]}
         @(re-posh/subscribe [:swig.subs.frame/get-frame id])

         children
         @(re-posh/subscribe [:swig.subs.element/get-children
                              id
                              [:swig.type/window
                               :swig.type/view
                               :swig.type/split]])]
     (methods/wrap props
                   [re/v-box
                    :attr {:id (str "frame-" id)}
                    :style {:position :absolute
                            :border-style "solid"
                            :flex "1 1 0%"
                            :box-shadow "5px 5px"
                            :width width
                            :height height
                            :top top
                            :left left}
                    :children (into (mapv methods/dispatch (:swig.operations/ops ops))
                                    (mapv methods/dispatch children))]))))
