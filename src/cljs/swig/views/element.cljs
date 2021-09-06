(ns swig.views.element
  (:require
   [swig.components.containers :refer [capability-container]]
   [swig.dispatch :as methods]
   [re-posh.core :as re-posh]
   [re-com.core :as re]))

(def swig-type? #{:swig.type/tab :swig.type/view :swig.type/split :swig.type/frame :swig.type/cell})

(defn element
  [{:keys [db/id]}]
  (let [props @(re-posh/subscribe [:swig.subs.element/get-props id])
        maximized-element (:swig.element/maximized-element props)
        invisible-styles {:visibility "hidden"
                          :display "none"
                          :flex "1 1 0%"}
        normal-styles {:flex "1 1 0%"}]
    (if (swig-type? (:swig/type props))
      (->> [re/h-box
            :style {:flex "1 1 0%"}
            :class "swig-element"
            :attr  {:id (str "swig-" id)
                    :on-mouse-down (fn [e]
                                     (.preventDefault e)
                                     (.stopPropagation e)
                                     (re-posh/dispatch [:swig.events.element/set-context id]))}
            :children
            [[re/box
              :style (if maximized-element normal-styles invisible-styles)
              :child (if maximized-element [element maximized-element] [:div])]
             [re/box
              :style (if maximized-element invisible-styles normal-styles)
              :child [methods/dispatch props]]]]
           (capability-container props)
           (methods/wrap props))
      [re/box
       :attr {:swigid id}
       :style {:flex "1 1 0%"}
       :class "swig-leaf"
       :child [methods/dispatch props]])))
