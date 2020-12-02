(ns swig.views.view
  (:require
   [swig.components.containers :refer [capability-container]]
   [swig.methods :as methods]
   [reagent.ratom :refer [reaction]]
   [reagent.core :as r]
   [re-posh.core :as re-posh]
   [re-com.core :as re]))

(defn tab-label-fn [tab]
  (methods/dispatch (:swig.tab/label tab)))

(defmethod methods/dispatch :swig.type/view
  [props]
  (let [view-id       (:db/id props)
        {view-ops :swig.view/ops view-type :swig.view/tab-type}
        @(re-posh/subscribe [:swig.subs.view/get-view-ops view-id])
        children      @(re-posh/subscribe [:swig.subs.element/get-children
                                           view-id
                                           [:swig.type/window
                                            :swig.type/view
                                            :swig.type/frame
                                            :swig.type/split]])
        tabs          (re-posh/subscribe [:swig.subs.view/get-tabs view-id])
        tabs-count    (count @tabs)
        active-tab    (re-posh/subscribe [:swig.subs.view/get-active-tab view-id])
        active-tab-id (:db/id @active-tab)
        _             (when-not (contains? (set (map :db/id @tabs)) active-tab-id)
                        (js/console.warn "No active-tab for view:" view-id))
        child
        (if (= tabs-count 0)
          [re/v-box
           :attr {:id (str "swig-" view-id)}
           :style {:flex "1 1 0%"}
           :children
           (doall
            (for [child children]
              ^{:key (str "child-" (:db/id child))}
              [methods/dispatch child]))]
          [re/v-box
           :gap "0px"
           :attr {:id (str "swig-" view-id)}
           :style {:flex "1 1 0%"}
           :children
           [[re/v-box
             :style {:flex "1 1 0%"}
             :children
             [^{:key (str "layout-h-tabs-" view-id)}
              [(case view-type :bar re/horizontal-bar-tabs re/horizontal-tabs)
               :style (if (or (= (count @tabs) 1)
                              (:swig.tab/fullscreen @active-tab))
                        {:visibility "hidden"
                         :display    "none"}
                        {})
               :model     (r/cursor active-tab [:db/id])
               :tabs      (reaction (sort-by :swig/index @tabs))
               :label-fn  tab-label-fn
               :id-fn     :db/id
               :view-id view-id
               :on-change (fn [tab-id]
                            (re-posh/dispatch [:swig.events.tab/set-active-tab view-id tab-id]))]
              (doall
               (for [tab (sort-by (fnil :swig/index 0) @tabs)
                     :let  [id (:db/id tab)]]
                 ^{:key (str "tab-" id)}
                 [re/box
                  :size (if (= id active-tab-id) "none" "0%")
                  :style (if (= id active-tab-id)
                           {:flex "1 1 0%"}
                           {:visibility "hidden"
                            :display    "none"})
                  :child [methods/dispatch tab]]))]]
            [re/v-box
             :style (if (or (= (count @tabs) 1)
                            (:swig.tab/fullscreen @active-tab))
                      {:visibility "hidden"
                       :display    "none"}
                      {})
             :children
             (doall
              (for [child children]
                ^{:key (str "window-" (:db/id child))}
                [methods/dispatch child]))]]])]
    (->> child
         (capability-container props)
         (methods/wrap props))))
