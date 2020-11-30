#?(:cljs
   (ns swig.views
     (:require [clojure.string :as str]
               [swig.components.containers :refer [capability-container mouse-xy]]
               [swig.components.tabs :refer [horizontal-tabs horizontal-bar-tabs]]
               [re-com.core :refer [box v-box h-split v-split scroller
                                    h-box md-icon-button line]]
               [re-posh.core :as re-posh]
               [reagent.core :as r]
               [reagent.ratom :refer [reaction]]
               [swig.events :as events]
               [swig.subs :as subs]))
   :clj
   (ns swig.views
     (:require
      [swig.events :as events]
      [swig.subs :as subs])))

(def root-view [:swig/ident :swig/root-view])

(defn swig-dispatch-fn
  ([props]
   (:swig/type props))
  ([props child]
   (:swig/type props)))

(defmulti dispatch #'swig-dispatch-fn)

(defmethod dispatch :default
  ([props]
   [:div (str "No method found for props:" props)])
  ([props child]
   [:div (str "No method found for props:" props)]))

#?(:cljs
   (defmethod dispatch :swig.type/frame
     ([props]
      (dispatch props nil))
     ([{:keys [:db/id]} child]
      (let [{:keys [:swig.frame/height
                    :swig.frame/width
                    :swig.frame/left
                    :swig.frame/top
                    :swig.frame/ops]
             :as props} @(re-posh/subscribe [::subs/get-frame id])
            children
            @(re-posh/subscribe [::subs/get-children id [:swig.type/window
                                                         :swig.type/view
                                                         :swig.type/split]])]
        [v-box
         :attr {:id (str "frame-" id)}
         :style {:position :absolute
                 :border-style "solid"
                 :flex "1 1 0%"
                 :box-shadow "5px 5px"
                 :width width
                 :height height
                 :top top
                 :left left}
         :children (into (mapv dispatch (:swig.operations/ops ops))
                         (mapv dispatch children))]))))

#?(:cljs
   (defmethod dispatch :swig.operation/divide-horizontal [{:keys [:db/id]}]
     ^{:key (str "id-" id)}
     [md-icon-button
      :size :smaller
      :md-icon-name "zmdi-swap"
      :on-click (fn [event]
                  (re-posh/dispatch [::events/divide-tab id :horizontal]))]))

#?(:cljs
   (defmethod dispatch :swig.operation/divide-vertical [{:keys [:db/id]}]
     ^{:key (str "id-" id)}
     [md-icon-button
      :size :smaller
      :md-icon-name "zmdi-swap-vertical"
      :on-click (fn [event]
                  (re-posh/dispatch [::events/divide-tab id :vertical]))]))

#?(:cljs
   (defmethod dispatch :swig.operation/fullscreen [{:keys [:db/id]}]
     [md-icon-button
      :size :smaller
      :md-icon-name "zmdi-fullscreen"
      :on-click (fn [event]
                  (re-posh/dispatch [::events/enter-fullscreen id]))]))

#?(:cljs
   (defmethod dispatch :swig.operation/delete [{:keys [:db/id]}]
     ^{:key (str "id-" id)}
     [md-icon-button
      :size :smaller
      :md-icon-name "zmdi-close"
      :on-click (fn [event]
                  (re-posh/dispatch [::events/kill-tab id]))]))

#?(:cljs
   (defmethod dispatch :swig.operation/swap [{:keys [:db/id]}]
     ^{:key (str "id-" id)}
     [md-icon-button
      :size :smaller
      :md-icon-name "zmdi-swap-vertical-circle"
      :on-click (fn [event]
                  (.stopPropagation event)
                  (re-posh/dispatch [::events/swap-views id]))]))

#?(:cljs
   (defmethod dispatch :swig.operation/join [{:keys [:db/id]}]
     ^{:keys (str "op.join-" id)}
     [md-icon-button
      :size :smaller
      :md-icon-name "zmdi-unfold-less"
      :on-click (fn [event]
                  (.stopPropagation event)
                  (re-posh/dispatch [::events/join-views id]))]))

#?(:cljs
   (defmethod dispatch :swig.operations/resize [{:keys [db/id] :as props}]
     (let [frame-id     @(re-posh/subscribe [::subs/op-get-frame id])
           container-id @(re-posh/subscribe [::subs/get-parent frame-id])]
       [md-icon-button
        :style {:position :absolute
                :bottom   "5px"
                :right    "5px"}
        :size :regular
        :md-icon-name "zmdi-code-setting"
        :attr {:on-mouse-down
               (fn [e]
                 (let [[left top] (mouse-xy e 1.0 (str "frame-" frame-id))]
                   (println "container-id:" frame-id)
                   (println "left,top:" left, top)
                   (re-posh/dispatch [::events/resize-start frame-id left top])))}])))

#?(:cljs
   (defmethod dispatch :swig.type/operations
     [{:keys [:db/id :swig.operations/ops] :as operations}]
     (let [{:keys [:sw]} operations]
       [h-box
        :children
        [[v-box
          :children
          (doall
           (for [op ops]
             (dispatch op)))]
         (when (seq ops)
           ^{:key (str "line-" id)}
           [line])]])))

#?(:cljs
   (defmethod dispatch :swig.type/tab
     ([{:keys [:db/id
               :swig/ident
               :swig.dispatch/handler
               :swig.container/capabilities] :as tab}]
      (let [child      (first @(re-posh/subscribe
                                [::subs/get-children id [:swig.type/split
                                                         :swig.type/frame
                                                         :swig.type/view]]))
            fns        (methods dispatch)
            handler-fn (get fns handler (get fns ident))
            ops        (:swig.tab/ops tab)
            container-id (str "tab-" id)]
        (capability-container
         tab
         [h-box
          :attr {:id (str "swig-" container-id)}
          :style {:flex "1 1 0%"}
          :children
          [^{:key (str "exit-fullscreen-" id)}
           [md-icon-button
            :style (if (:swig.tab/fullscreen tab)
                     {}
                     {:visibility "hidden"
                      :display    "none"})
            :size :smaller
            :md-icon-name "zmdi-close"
            :on-click (fn [event]
                        (re-posh/dispatch [::events/exit-fullscreen id]))]
           (when ops
             (dispatch ops))
           (when handler-fn
             (if child
               (handler-fn tab child)
               (handler-fn tab)))
           (when child
             (dispatch child))]])))))

#?(:cljs
   (defmethod dispatch :swig.type/window
     ([{:keys [:swig/ident] :as props}]
      ((get-method dispatch ident) props))
     ([{:keys [:swig/ident] :as props} child]
      ((get-method dispatch ident) props child))))

#?(:cljs
   (defn tab-label-fn [tab]
     (dispatch (:swig.tab/label tab))))

#?(:cljs
   (defmethod dispatch :swig.type/view
     [{:keys [:swig.dispatch/handler] :as props}]
     (let [view-id       (:db/id props)
           {view-ops :swig.view/ops view-type :swig.view/tab-type}
           @(re-posh/subscribe [::subs/get-view-ops view-id])
           children      @(re-posh/subscribe [::subs/get-children view-id [:swig.type/window
                                                                           :swig.type/view
                                                                           :swig.type/frame
                                                                           :swig.type/split]])
           tabs          (re-posh/subscribe [::subs/get-tabs view-id])
           tabs-count    (count @tabs)
           active-tab    (re-posh/subscribe [::subs/get-active-tab view-id])
           active-tab-id (:db/id @active-tab)
           _             (when-not (contains? (set (map :db/id @tabs)) active-tab-id)
                           (js/console.warn "No active-tab for view:" view-id))
           child
           (if (= tabs-count 0)
             [v-box
              :attr {:id (str "swig-" view-id)}
              :style {:flex "1 1 0%"}
              :children
              (doall
               (for [child children]
                 ^{:key (str "child-" (:db/id child))}
                 [dispatch child]))]
             [v-box
              :gap "0px"
              :attr {:id (str "swig-" view-id)}
              :style {:flex "1 1 0%"}
              :children
              [[v-box
                :style {:flex "1 1 0%"}
                :children
                [^{:key (str "layout-h-tabs-" view-id)}
                 [(case view-type :bar horizontal-bar-tabs horizontal-tabs)
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
                               (re-posh/dispatch [::events/set-active-tab view-id tab-id]))]
                 (doall
                  (for [tab (sort-by (fnil :swig/index 0) @tabs)
                        :let  [id (:db/id tab)]]
                    ^{:key (str "tab-" id)}
                    [box
                     :size (if (= id active-tab-id) "none" "0%")
                     :style (if (= id active-tab-id)
                              {:flex "1 1 0%"}
                              {:visibility "hidden"
                               :display    "none"})
                     :child [dispatch tab]]))]]
               [v-box
                :style (if (or (= (count @tabs) 1)
                               (:swig.tab/fullscreen @active-tab))
                         {:visibility "hidden"
                          :display    "none"}
                         {})
                :children
                (doall
                 (for [child children]
                   ^{:key (str "window-" (:db/id child))}
                   [dispatch child]))]]])]
       (capability-container
        props
        (if handler
          ((get-method dispatch handler) props child)
          child)))))

#?(:cljs
   (defmethod dispatch :swig.type/split
     [{:keys [:swig.dispatch/handler] :as props}]
     (let [split-id (:db/id props)
           children (sort-by :swig/index
                             @(re-posh/subscribe [::subs/get-children split-id [:swig.type/view
                                                                                :swig.type/tab
                                                                                :swig.type/split]]))
           split    @(re-posh/subscribe [::subs/get-split split-id])
           ops      (set @(re-posh/subscribe [::subs/get-op-names (->> split :swig.split/ops (map :db/id))]))
           child
           [(case (:swig.split/orientation split) :horizontal h-split :vertical v-split)
            :on-split-change #(re-posh/dispatch [::events/set-split-percent (:db/id split) %])
            :style           {:flex   "1 1 0%"
                              :margin "0px"}
            :attr            {:on-click (when (contains? ops :operation/join)
                                          (fn [event]
                                            (when (and (-> event .-ctrlKey)
                                                       (-> event .-target .-className (str/includes? "split-splitter")))
                                              (.stopPropagation event)
                                              (re-posh/dispatch [::events/join-views split-id]))))}
            :initial-split   (:swig.split/split-percent split)
            :panel-1         [dispatch (first children)]
            :panel-2         [dispatch (second children)]]]
      (if handler
        ((get-method dispatch handler) props child)
        child))))

#?(:cljs
   (defn root-component [view-id]
     (let [elem @(re-posh/subscribe [::subs/get-element view-id])]
       [v-box
        :height "100vh"
        :width "100vw"
        :children
        [[dispatch elem]]]))
 :clj
 (defn root-component [view-id]
   (throw (Exception. "Not implemented for clj."))))
