(ns swig.three.compile
  (:require
   [three :as three]
   [three-orbitcontrols :as OrbitControls]
   [oops.core :refer [oget oset!]]))

(def ^:dynamic *dom-node* nil)

(derive :swig.type/three.perspective-camera :swig.type/three.camera)
(derive :swig.type/three.ortho-camera       :swig.type/three.camera)
(derive :swig.type/three.sphere             :swig.type/object)
(derive :swig.type/three.box                :swig.type/object)
(derive :swig.type/three.cylinder           :swig.type/object)

(defn find-elem-idx [children type]
  (loop [[c & cs] children
         idx 0]
    (cond (isa? (:swig/type c) type) idx
          (nil? cs) nil
          :else (recur cs (inc idx)))))

(defmulti construct-scene :swig/type)


(defmethod construct-scene :swig.type/three.scene
  [{:keys [swig/children]}]
  (let [scene (three/Scene.)
        elems (map construct-scene children)]
    (doseq [{:keys [three/obj]} elems]
      (.add scene obj))
    scene))


(defmethod construct-scene :swig.type/three.orbit-controls
  [{:keys [three.orbit-controls/enableDamping
           three.orbit-controls/autoRotate
           three.orbit-controls/zoomSpeed
           three.orbit-controls/minDistance
           three.orbit-controls/maxDistance
           three.orbit-controls/minPolarAngle
           three.orbit-controls/maxPolarAngle
           swig/children]}]
  (let [camera-idx (find-elem-idx children :swig.type/three.camera)
        elems      (mapv construct-scene children)
        camera     (:three/obj (nth elems camera-idx))]
    (doto (cond-> (OrbitControls. camera *dom-node*)
            enableDamping (oset! "enableDamping" enableDamping)
            autoRotate    (oset! "autoRotate" autoRotate)
            zoomSpeed     (oset! "zoomSpeed" zoomSpeed)
            minDistance   (oset! "minDistance" minDistance)
            maxDistance   (oset! "maxDistance" maxDistance)
            minPolarAngle (oset! "minPolarAngle" minPolarAngle)
            maxPolarAngle (oset! "maxPolarAngle" maxPolarAngle))
      (.addEventListener "end" #(println "end"))
      (.addEventListener "change" #(println "change")))))


(defmethod construct-scene :swig.type/three.perspective-camera
  [{:keys [three.perspective-camera/fov
           three.perspective-camera/aspect
           three.perspective-camera/near
           three.perspective-camera/far
           three.perspective-camera/active
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or {position #js [0 0 0]
         rotation #js [0 0 0]
         scale 1.0}
    :as props}]
  (let [elems (map construct-scene children)
        cam   (three/PerspectiveCamera.)]
    (set! (.-position cam) (into-array position))
    (set! (.-rotation cam) (into-array rotation))
    (set! (.-scale cam) scale)
    (cond-> cam
      fov    (oset! "fov" fov)
      aspect (oset! "aspect" aspect)
      near   (oset! "near" near)
      far    (oset! "far" far)
      active (oset! "active" active))
    (doseq [{:keys [three/obj]} elems]
      (.add cam obj))
    (assoc props :three/obj cam)))


(defmethod construct-scene :swig.type/three.plane
  [{:keys [three.plane/width
           three.plane/height
           three.plane/depth
           three.plane/width-segments
           three.plane/height-segments
           three.object/position
           three.object/rotation
           three.object/scale
           three.plane/material
           swig/children]
    :or   {width           1.0
           height          1.0
           depth           1.0
           width-segments  1
           height-segments 1
           depth-segments  1
           position        #js [1 1 1]
           rotation        #js [1 1 1]
           scale           1.0}
    :as   props}]
  (let [elems (map construct-scene children)
        plane (three/Mesh (three/PlaneGeometry. width height width-segments height-segments)
                          (three/MeshBasicMaterial.))]
    (set! (.-position plane) position)
    (set! (.-rotation plane) rotation)
    (set! (.-scale plane) scale)
    (doseq [{:keys [three/obj]} elems]
      (.add plane obj))
    (assoc props :three/obj plane)))


(defmethod construct-scene :swig.type/three.object
  [{:keys [three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or {position #js [1 1 1]
         rotation #js [1 1 1]
         scale 1.0}
    :as props}]
  (let [elems (map construct-scene children)
        object (three/Object3D.)]
    (set! (.-position object) (into-array position))
    (set! (.-rotation object) (into-array rotation))
    (set! (.-scale object) scale)
    (doseq [{:keys [three/obj]} elems]
      (.add object obj))
    (assoc props :three/obj object)))


(defmethod construct-scene :swig.type/three.sphere
  [{:keys [three.sphere/radius
           three.sphere/width-segments
           three.sphere/height-segments
           three.sphere/phi-start
           three.sphere/phi-length
           three.sphere/theta-start
           three.sphere/theta-length
           three.object/position
           three.object/rotation
           three.object/scale
           three.object/material
           swig/children]
    :or   {radius          1.0
           width-segments  8
           height-segments 6
           phi-start       0
           phi-length      (* Math/PI 2)
           theta-start     0
           theta-length    Math/PI
           position #js [0 0 0]
           rotation #js [0 0 0]}
    :as props}]
  (let [elems (map construct-scene children)
        mesh (three/Mesh. (three/SphereGeometry. radius
                                                 width-segments
                                                 height-segments
                                                 phi-start
                                                 phi-length
                                                 theta-start
                                                 theta-length)
                          (three/MeshBasicMaterial.))]
    (set! (.-position mesh) position)
    (set! (.-rotation mesh) rotation)
    (set! (.-scale mesh) scale)
    (doseq [{:keys [three/obj]} elems]
        (.add mesh obj))
    (assoc props :three/obj mesh)))


(defmethod construct-scene :swig.type/three.box
  [{:keys [three.box/width
           three.box/height
           three.box/depth
           three.box/width-segments
           three.box/height-segments
           three.box/depth-segments
           three.box/material
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {width           1.0
           height          1.0
           depth           1.0
           width-segments  1
           height-segments 1
           depth-segments  1
           position        #js [0 0 0]
           rotation        #js [0 0 0]
           scale           1.0}
    :as   props}]
  (let [elems (map construct-scene children)
        box   (three/Mesh. (three/BoxGeometry. width
                                               height
                                               depth
                                               width-segments
                                               height-segments
                                               depth-segments)
                           (three/MeshBasicMaterial.))]
    (set! (.-position box) position)
    (set! (.-rotation box) rotation)
    (set! (.-scale box) scale)
    (doseq [{:keys [three/obj]} elems]
      (.add box obj))
    (assoc props :three/obj box)))
