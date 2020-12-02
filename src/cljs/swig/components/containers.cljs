(ns swig.components.containers
  (:require
   [swig.utils :refer [mouse-xy to-tlwh]]
   [swig.methods :as methods]
   [swig.subs :as subs]
   [swig.events :as events]
   [re-posh.core :as re-posh]
   [re-com.core :as re]))

(defn selection [active? start end style]
  (let [[t l w h] (to-tlwh @start @end)]
    (when @active?
      [:rect {:x l :y t :width w :height h :style style}])))

(defmethod methods/capability-handler :swig.capability/drag
  [child _ {:keys [db/id]}]
  (let [container-id (str "drag-" id)
        frame-id     @(re-posh/subscribe [:swig.subs.drag/drag-frame-id id])
        [offset-left offset-top] @(re-posh/subscribe [::subs/drag-offsets frame-id])]
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
                 (let [[left top] (mouse-xy e 1.0 container-id)]
                   (re-posh/dispatch [:swig.events.drag/drag-frame frame-id
                                      (- left offset-left)
                                      (- top offset-top)]))
                 e))

             :on-mouse-up
             (fn [e]
               (.preventDefault e)
               (when frame-id
                 (let [[left top] (mouse-xy e 1.0 container-id)]
                   (re-posh/dispatch [:swig.events.drag/drag-frame frame-id
                                      (- left offset-left)
                                      (- top offset-top)])
                   (re-posh/dispatch [::events/drag-stop frame-id])))
               e)}
     :child child]))

(defmethod methods/capability-handler :swig.capability/resize
  [child _ {:keys [db/id]}]
  (let [container-id             (str "select-" id)
        frame-id                 @(re-posh/subscribe [::subs/resize-frame-id id])
        {:keys [swig.capability.resize/start-left
                swig.capability.resize/start-top
                swig.frame/left
                swig.frame/top]}
        @(re-posh/subscribe [:swig.subs.resize/resize-start-pose frame-id])]
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
                         (re-posh/dispatch [::events/resize-stop frame-id])
                         (when-not (= (.-id elem) container-id)
                           (recur (.-parentElement elem))))))))
               e)

             :on-mouse-move
             (fn [e]
               (when (and frame-id start-left start-top)
                 (.preventDefault e)
                 (let [[mouse-left mouse-top] (mouse-xy e 1.0 container-id)]
                   (re-posh/dispatch [:swig.events.resize/resize-frame frame-id
                                      (+ (- mouse-left left) start-left)
                                      (+ (- mouse-top top) start-top)])))
               e)

             :on-mouse-up
             (fn [e]
               (.preventDefault e)
               (when frame-id
                 (let [[mouse-left mouse-top] (mouse-xy e 1.0 container-id)]
                   (re-posh/dispatch [:swig.events.resize/resize-frame frame-id
                                      (+ (- mouse-left left) start-left)
                                      (+ (- mouse-top top) start-top)])
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
