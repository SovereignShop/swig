#?(:cljs
   (ns swig.views
     (:require [clojure.string :as str]
               [re-com.core :refer [box v-box horizontal-tabs h-split v-split scroller
                                    h-box md-icon-button line gap horizontal-bar-tabs]]
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

#_(defn open-debugger-window
  "Originally copied from re-frisk.devtool/open-debugger-window"
  [{:keys [width height top left] :as dimensions}]
  (let [doc-title        js/document.title
        new-window-title (goog.string/escapeString (str "re-frame-10x | " doc-title))
        new-window-html  (str "<head><title>"
                              new-window-title
                              "</title></head><body style=\"margin: 0px;\"><div id=\"--re-frame-10x--\" class=\"external-window\"></div></body>")]
    ;; We would like to set the windows left and top positions to match the monitor that it was on previously, but Chrome doesn't give us
    ;; control over this, it will only position it within the same display that it was popped out on.
    (if-let [w (js/window.open "about:blank" "re-frame-10x-popout"
                               (str "width=" width ",height=" height ",left=" left ",top=" top
                                    ",resizable=yes,scrollbars=yes,status=no,directories=no,toolbar=no,menubar=no"))]
      (let [d (.-document w)]
        ;; We had to comment out the following unmountComponentAtNode as it causes a React exception we assume
        ;; because React says el is not a root container that it knows about.
        ;; In theory by not freeing up the resources associated with this container (e.g. event handlers) we may be
        ;; creating memory leaks. However with observation of the heap in developer tools we cannot see any significant
        ;; unbounded growth in memory usage.
        ;(when-let [el (.getElementById d "--re-frame-10x--")]
        ;  (r/unmount-component-at-node el)))
        (.open d)
        (.write d new-window-html)
        (goog.object/set w "onload" #(mount w d))
        (.close d)
        true)
      false)))

(defmulti dispatch #'swig-dispatch-fn)

#?(:cljs
   (defn tab-operations
     ([tab] (tab-operations tab 1))
     ([{:keys [:db/id :swig/ident :swig.dispatch/handler] :as tab} tabs-count]
      (let [child      (first @(re-posh/subscribe [::subs/get-children id [:swig.type/split
                                                                           :swig.type/view]]))
            fns        (methods dispatch)
            handler-fn (get fns handler (get fns ident))
            ops      (set @(re-posh/subscribe [::subs/get-op-names (->> tab :swig.tab/ops (map :db/id))]))]
        [h-box
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
          (when (seq ops)
            [v-box
             :style (if (:swig.tab/fullscreen tab)
                      {:visibility "hidden"
                       :display    "none"}
                      {})
             :gap "5px"
             :children
             [(when (contains? ops :operation/fullscreen)
                ^{:key (str "ui.tabs.ops.fullscreen-" id)}
                [md-icon-button
                 :size :smaller
                 :md-icon-name "zmdi-fullscreen"
                 :on-click (fn [event]
                             (re-posh/dispatch [::events/enter-fullscreen id]))])
              (when (and (contains? ops :operation/divide-vertical)
                         (> tabs-count 1))
                ^{:key (str "ui.tabs.ops.divide-vertical-" id)}
                [md-icon-button
                 :size :smaller
                 :md-icon-name "zmdi-swap-vertical"
                 :on-click (fn [event]
                             (re-posh/dispatch [::events/divide-tab id :vertical]))])
              (when (and (contains? ops :operation/divide-horizontal)
                         (> tabs-count 1))
                ^{:key (str "ui.tabs.ops.divide-horizontal-" id)}
                [md-icon-button
                 :size :smaller
                 :md-icon-name "zmdi-swap"
                 :on-click (fn [event]
                             (re-posh/dispatch [::events/divide-tab id :horizontal]))])
              (when (and (contains? ops :operation/swap)
                         (> tabs-count 1))
                ^{:key (str "ui.tabs.ops.delete.swap-" id)}
                [md-icon-button
                 :size :smaller
                 :md-icon-name "zmdi-swap-vertical-circle"
                 :on-click (fn [event]
                             (.stopPropagation event)
                             (re-posh/dispatch [::events/swap-views id]))])
              (when (contains? ops :operation/delete)
                ^{:key (str "ui.tabs.ops.delete-" id)}
                [md-icon-button
                 :size :smaller
                 :md-icon-name "zmdi-close"
                 :on-click (fn [event]
                             (re-posh/dispatch [::events/kill-tab id]))])]])
          (when (seq ops)
            ^{:key (str "line-" id)}
            [line])
          (if handler-fn
            (if child
              (handler-fn tab child)
              (handler-fn tab))
            (if child
              (dispatch child)
              [:div "No handler or child found for tab:" tab]))]]))))

(defmethod dispatch :default
  ([props]
   [:div (str "No method found for props:" props)])
  ([props child]
   [:div (str "No method found for props:" props)]))

#?(:cljs
   (defmethod dispatch :swig.type/tab [tab] [tab-operations tab]))

#?(:cljs
   (defmethod dispatch :swig.type/window
     ([{:keys [:swig/ident] :as props}]
      ((get-method dispatch ident) props))
     ([{:keys [:swig/ident] :as props} child]
      ((get-method dispatch ident) props child))))

#?(:cljs
   (defn tab-label-fn [tab]
     (let [{:keys [:db/id]} (:swig.tab/label tab)]
       (dispatch @(re-posh/subscribe [::subs/get-cell id])))))


#?(:cljs
   (defmethod dispatch :swig.type/view
     [{:keys [:swig.dispatch/handler] :as props}]
     (let [view-id       (:db/id props)
           {view-ops :swig.view/ops view-type :swig.view/tab-type}
           @(re-posh/subscribe [::subs/get-view-ops view-id])
           children      @(re-posh/subscribe [::subs/get-children view-id [:swig.type/window
                                                                           :swig.type/view
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
              :style {:flex "1 1 0%"}
              :children
              (doall
               (for [child children]
                 ^{:key (str "child-" (:db/id child))}
                 [dispatch child]))]
             [v-box
              :gap "0px"
              :style {:flex "1 1 0%"}
              :children
              [[box
                :size "1"
                :style {:flex "1 1 0%"}
                :child
                [v-box
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
                   :on-change (fn [tab-id]
                                (re-posh/dispatch [::events/set-active-tab view-id tab-id]))]
                  (doall
                   (for [a-tab (sort-by (fnil :swig/index 0) @tabs)
                         :let  [id (:db/id a-tab)]]
                     ^{:key (str "tab-" id)}
                     [box
                      :size (if (= id active-tab-id) "none" "0%")
                      :style (if (= id active-tab-id)
                               {:flex "1 1 0%"}
                               {:visibility "hidden"
                                :display    "none"})
                      :child [tab-operations a-tab tabs-count]]))]]]
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
       (if handler
         ((get-method dispatch handler) props child)
         child))))

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
            :style {:flex   "1 1 0%"
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

(comment

  (def app
    (init-layout
     [root
      [split
       [view
        [tab :child]
        [tab :child]]
       [view
        [tab :child]
        [tab :child]]]]))

  )
