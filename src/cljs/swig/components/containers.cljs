(ns swig.components.containers
  (:require
   [swig.utils :refer [mouse-xy to-tlwh]]
   [swig.dispatch :as methods]
   [re-posh.core :as re-posh]
   [re-com.core :as re])
  (:import
   [goog.async Debouncer]))

(defn selection [active? start end style]
  (let [[t l w h] (to-tlwh @start @end)]
    (when @active?
      [:rect {:x l :y t :width w :height h :style style}])))

(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    ;; We use apply here to support functions of various arities
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

(defn drag-function [frame-id offset-left offset-top left top]
  (re-posh/dispatch [:swig.events.drag/drag-frame frame-id
                     (- left offset-left)
                     (- top offset-top)
                     :no-save? true]))

(def debounced-drag
  (debounce drag-function 0.1))

(defn distance [x1 y1 x2 y2]
  (Math/sqrt (Math/pow (- x2 x1) 2)
             (Math/pow (- y2 y1) 2)))

(defmethod methods/capability-handler :swig.capability/drag
  [child _ {:keys [db/id]}]
  (let [container-id             (str "drag-" id)
        frame-id                 @(re-posh/subscribe [:swig.subs.drag/drag-frame-id id])
        [offset-left offset-top] @(re-posh/subscribe [:swig.subs.drag/drag-offsets frame-id])
        drag-fn (partial drag-function frame-id offset-left offset-top)
        debounced-drag-fn (partial debounced-drag frame-id offset-left offset-top)
        xy (volatile! nil)]
    [re/box
     :style {:flex "1 1 0%"}
     :attr  {:id container-id
             :on-mouse-out
             (fn [e]
               (when frame-id
                 (let [elem (.-relatedTarget e)]
                   (if-not elem
                     (re-posh/dispatch [:swig.events.drag/drag-stop frame-id])
                     (loop [elem (.-parentElement elem)]
                       (if-not elem
                         (re-posh/dispatch [:swig.events.drag/drag-stop frame-id])
                         (when-not (= (.-id elem) container-id)
                           (recur (.-parentElement elem))))))))
               e)

             :on-mouse-move
             (fn [e]
               (when (and frame-id offset-left offset-top)
                 (.preventDefault e)
                 (let [[pl pt] @xy
                       [left top] (vreset! xy (mouse-xy e 1.0 container-id))]
                   (if (and pt (< (distance pl pt left top) 20))
                     (debounced-drag-fn left top)
                     (drag-fn left top)))
                 e))

             :on-mouse-up
             (fn [e]
               (.preventDefault e)
               (when frame-id
                 (let [[left top] (mouse-xy e 1.0 container-id)]
                   (re-posh/dispatch [:swig.events.drag/drag-frame frame-id
                                      (- left offset-left)
                                      (- top offset-top)
                                      :no-save? false])
                   (re-posh/dispatch [:swig.events.drag/drag-stop frame-id])))
               e)}
     :child child]))

(def debounced-resize
  (debounce (fn [frame-id start-left start-top left top mouse-left mouse-top]
              (re-posh/dispatch [:swig.events.resize/resize-frame frame-id
                                 (+ (- mouse-left left) start-left)
                                 (+ (- mouse-top top) start-top)
                                 :no-save? true]))
            0.2))

(defmethod methods/capability-handler :swig.capability/resize
  [child _ {:keys [db/id]}]
  (let [container-id             (str "select-" id)
        frame-id                 @(re-posh/subscribe [:swig.subs.resize/resize-frame-id id])
        {:keys [swig.capability.resize/start-left
                swig.capability.resize/start-top
                swig.frame/left
                swig.frame/top]}
        @(re-posh/subscribe [:swig.subs.resize/resize-start-pose frame-id])
        resize-fn (partial debounced-resize frame-id start-left start-top left top)]
    [re/box
     :style {:flex "1 1 0%"}
     :attr  {:id container-id
             :on-mouse-out
             (fn [e]
               (when frame-id
                 (let [elem (.-relatedTarget e)]
                   (if-not elem
                     (re-posh/dispatch [:swig.events.resize/resize-stop frame-id])
                     (loop [elem (.-parentElement elem)]
                       (if-not elem
                         (re-posh/dispatch [:swig.events.resize/resize-stop frame-id])
                         (when-not (= (.-id elem) container-id)
                           (recur (.-parentElement elem))))))))
               e)

             :on-mouse-move
             (fn [e]
               (when (and frame-id start-left start-top)
                 (.preventDefault e)
                 (let [[mouse-left mouse-top] (mouse-xy e 1.0 container-id)]
                   (resize-fn mouse-left mouse-top)))
               e)

             :on-mouse-up
             (fn [e]
               (.preventDefault e)
               (when frame-id
                 (let [[mouse-left mouse-top] (mouse-xy e 1.0 container-id)]
                   (re-posh/dispatch [:swig.events.resize/resize-frame frame-id
                                      (+ (- mouse-left left) start-left)
                                      (+ (- mouse-top top) start-top)
                                      :no-save? false])
                   (re-posh/dispatch [:swig.events.resize/resize-stop frame-id])))
               e)}
     :child child]))

(defmethod methods/capability-handler :swig.capability/select
  [child _ {:keys [db/id]}]
  (let [container-id (str "select-" id)
        frame-id     @(re-posh.core/subscribe [:swig.subs.select/select-frame-id])]
    [re/box
     :attr {:id container-id
            :on-mouse-out
            (fn [e] e)
            :on-mouse-down
            (fn [e] e)
            :on-mouse-up
            (fn [e] e)}
     :child child]))

(defn capability-container
  [{:keys [:swig.container/capabilities] :as props} child]
  (reduce #(methods/capability-handler %1 %2 props) child capabilities))
