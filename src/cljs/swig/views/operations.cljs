(ns swig.views.operations
  (:require
   [swig.utils :refer [mouse-xy]]
   [swig.methods :as methods]
   [re-posh.core :as re-posh]
   [re-com.core :as re]))

(defmethod methods/dispatch :swig.operation/divide-horizontal [{:keys [db/id]}]
  ^{:key (str "divide-horizontal-" id)}
  [re/md-icon-button
   :size :smaller
   :md-icon-name "zmdi-swap"
   :on-click (fn [_]
               (re-posh/dispatch [:swig.events.tab/divide-tab id :horizontal]))])

(defmethod methods/dispatch :swig.operation/divide-vertical [{:keys [db/id]}]
  ^{:key (str "divide-horizontal-" id)}
  [re/md-icon-button
   :size :smaller
   :md-icon-name "zmdi-swap-vertical"
   :on-click (fn [_]
               (re-posh/dispatch [:swig.events.tab/divide-tab id :vertical]))])

(defmethod methods/dispatch :swig.operation/fullscreen [{:keys [db/id]}]
  ^{:key (str "fullscreen-" id)}
  [re/md-icon-button
   :size :smaller
   :md-icon-name "zmdi-fullscreen"
   :on-click (fn [_]
               (re-posh/dispatch [:swig.events.tab/enter-fullscreen id]))])

(defmethod methods/dispatch :swig.operation/delete [{:keys [db/id]}]
  ^{:key (str "delete-" id)}
  [re/md-icon-button
   :size :smaller
   :md-icon-name "zmdi-close"
   :on-click (fn [_]
               (re-posh/dispatch [:swig.events.tab/kill-tab id]))])

(defmethod methods/dispatch :swig.operation/swap [{:keys [db/id]}]
  ^{:key (str "swap-" id)}
  [re/md-icon-button
   :size :smaller
   :md-icon-name "zmdi-swap-vertical-circle"
   :on-click (fn [event]
               (.stopPropagation event)
               (re-posh/dispatch [:swig.events.view/swap-views id]))])

(defmethod methods/dispatch :swig.operation/join [{:keys [db/id]}]
  ^{:keys (str "join-" id)}
  [re/md-icon-button
   :size :smaller
   :md-icon-name "zmdi-unfold-less"
   :on-click (fn [event]
               (.stopPropagation event)
               (re-posh/dispatch [:swig.events.view/join-views id]))])

(defmethod methods/dispatch :swig.operations/resize [{:keys [db/id]}]
  (let [frame-id @(re-posh/subscribe [:swig.subs.operations/op-get-frame id])]
    ^{:key (str "resize-" id)}
    [re/md-icon-button
     :style {:position :absolute
             :bottom   "5px"
             :right    "5px"}
     :size :regular
     :md-icon-name "zmdi-code-setting"
     :attr {:on-mouse-down
            (fn [e]
              (let [[left top] (mouse-xy e 1.0 (str "frame-" frame-id))]
                (re-posh/dispatch [:swig.events.resize/resize-start frame-id left top])))}]))

(defmethod methods/dispatch :swig.type/operations
  [{:keys [:db/id :swig.operations/ops]}]
  [re/h-box
   :children
   [[re/v-box
     :children
     (doall
      (for [op ops]
        (methods/dispatch op)))]
    (when (seq ops)
      ^{:key (str "line-" id)}
      [re/line])]])
