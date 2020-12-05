(ns swig.utils)


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
           [(* (- (.-clientX e) (.-left r)) (/ 1 scale))
            (* (- (.-clientY e) (.-top r)) (/ 1 scale))])
         (recur (.-parentElement rect)))))))
