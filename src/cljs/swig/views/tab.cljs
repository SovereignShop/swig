(ns ^:dev/always swig.views.tab
  (:require
   [swig.components.containers :refer [capability-container]]
   [swig.dispatch :as methods]
   [re-posh.core :as re-posh]
   [re-com.core :as re]))

(defmethod methods/dispatch :swig.type/tab
  ([{:keys [:db/id] :as tab}]
   (let [child        (first @(re-posh/subscribe
                               [:swig.subs.element/get-children
                                id
                                [:swig.type/split
                                 :swig.type/frame
                                 :swig.type/view
                                 :skyhook.type/editor
                                 :swig.type/cljscad-viewer]]))
         ops          (:swig.element/ops tab)
         container-id (str "tab-" id)]
     (->> [re/h-box
           :attr  {:id (str "swig-" container-id)
                   :on-mouse-down (fn [e]
                                    (.preventDefault e)
                                    (.stopPropagation e)
                                    (re-posh/dispatch [:swig.events.tab/select-tab id]))}
           :style {:flex "1 1 0%"}
           :children
           [^{:key (str "exit-fullscreen-" id)}
            [re/md-icon-button
             :style (if (:swig.tab/fullscreen tab)
                      {}
                      {:visibility "hidden"
                       :display    "none"})
             :size :smaller
             :md-icon-name "zmdi-close"
             :on-click (fn [_]
                         (re-posh/dispatch [:swig.events.tab/exit-fullscreen id]))]
            (when-not (:swig.tab/fullscreen tab)
              (when ops
                (methods/dispatch ops)))
            (when child
              (methods/dispatch child))
            (when (:swig/ident tab)
              (methods/wrap (dissoc tab :swig.tab/fullscreen)))]]
          (capability-container tab)))))
