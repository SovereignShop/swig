(ns swig.components.containers
  (:require
   [swig.subs :as subs]
   [swig.events :as events]
   [re-posh.core :as re-posh]
   [re-com.core :refer [box]]))

(defn to-tlwh [[x1 y1] [x2 y2]]
  (let [dx (- x2 x1)
        dy (- y2 y1)
        t (min y1 y2)
        l (min x1 x2)
        res [t l (Math/abs dx) (Math/abs dy)]]
    res))

(defn mouse-xy
  ([e scale]
   (let [rect (.getBoundingClientRect (.-target e))]
     [(* (- (.-clientX e) (.-left rect)) (/ 1 scale))
      (* (- (.-clientY e) (.-top rect)) (/ 1 scale))]))
  ([e scale container-id]
   (loop [rect (.-target e)]
     (if (nil? rect)
       (throw (js/Error. "No parent"))
       (if (= (.-id rect) container-id)
         (let [r (.getBoundingClientRect rect)]
           (do (println "MOUSE:" (.-clientX e) (.-left r))
               [(* (- (.-clientX e) (.-left r)) (/ 1 scale))
                (* (- (.-clientY e) (.-top r)) (/ 1 scale))]))
         (recur (.-parentElement rect)))))))

(defn selection [active? start end style]
  (let [[t l w h] (to-tlwh @start @end)]
    (when @active?
      [:rect {:x l :y t :width w :height h :style style}])))

(defmulti capability-handler (comp second list))

(defmethod capability-handler :swig.capability/drag
  [child _ {:keys [db/id]}]
  (let [container-id (str "drag-" id)
        frame-id     @(re-posh/subscribe [::subs/drag-frame-id id])
        [offset-left offset-top] @(re-posh/subscribe [::subs/drag-offsets frame-id])]
    [box
     :style {:flex "1 1 0%"}
     :attr  {:id container-id
             :on-mouse-out
             (fn [e]
               (when frame-id
                 (let [elem (.-relatedTarget e)]
                   (if-not elem
                     (re-posh/dispatch [::events/drag-stop frame-id])
                     (loop [elem (.-parentElement elem)]
                       (if-not elem
                         (re-posh/dispatch [::events/drag-stop frame-id])
                         (when-not (= (.-id elem) container-id)
                           (recur (.-parentElement elem))))))))
               e)

             :on-mouse-move
             (fn [e]
               (when (and frame-id offset-left offset-top)
                 (.preventDefault e)
                 (let [[left top] (mouse-xy e 1.0 container-id)]
                   (re-posh/dispatch [::events/drag-frame frame-id
                                      (- left offset-left)
                                      (- top offset-top)]))
                 e))

             :on-mouse-up
             (fn [e]
               (.preventDefault e)
               (when frame-id
                 (let [[left top] (mouse-xy e 1.0 container-id)]
                   (re-posh/dispatch [::events/drag-frame frame-id
                                      (- left offset-left)
                                      (- top offset-top)])
                   (re-posh/dispatch [::events/drag-stop frame-id])))
               e)}
     :child child]))

(defmethod capability-handler :swig.capability/resize
  [child _ {:keys [db/id]}]
  (let [container-id             (str "select-" id)
        frame-id                 @(re-posh/subscribe [::subs/resize-frame-id id])
        {:keys [swig.capability.resize/start-left
                swig.capability.resize/start-top
                swig.frame/left
                swig.frame/top]}
        @(re-posh/subscribe [::subs/resize-start-pose frame-id])]
    [box
     :style {:flex "1 1 0%"}
     :attr  {:id container-id
             :on-mouse-out
             (fn [e]
               (when frame-id
                 (let [elem (.-relatedTarget e)]
                   (if-not elem
                     (re-posh/dispatch [::events/resize-stop frame-id])
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
                   (re-posh/dispatch [::events/resize-frame frame-id
                                      (+ (- mouse-left left) start-left)
                                      (+ (- mouse-top top) start-top)]))
                 e))

             :on-mouse-up
             (fn [e]
               (.preventDefault e)
               (when frame-id
                 (let [[mouse-left mouse-top] (mouse-xy e 1.0 container-id)]
                   (re-posh/dispatch [::events/resize-frame frame-id
                                      (+ (- mouse-left left) start-left)
                                      (+ (- mouse-top top) start-top)])
                   (re-posh/dispatch [::events/resize-stop frame-id])))
               e)}
     :child child]))

(defmethod capability-handler :swig.capability/select
  [child _ {:keys [db/id]}]
  (let [container-id (str "select-" id)
        frame-id     @(re-posh.core/subscribe [::subs/select-frame-id])]
    [box
     :attr {:id container-id
            :on-mouse-out
            (fn [e] e)
            :on-mouse-down
            (fn [e] e)
            :on-mouse-up
            (fn [e] e)}
     :child child]))

(defn capability-container
  [{:keys [:swig.container/capabilities :db/id] :as props} child]
  (println "CAPABILITIES: " capabilities)
  (reduce #(capability-handler %1 %2 props) child capabilities))
