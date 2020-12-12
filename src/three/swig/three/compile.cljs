(ns swig.three.compile
  (:require
   [goog.dom :as gdom]
   [swig.parser :as parser]
   [three :as three]
   [swig.three.helpers :as helpers]
   [swig.three.methods :as methods]
   [three-orbitcontrols :as OrbitControls]
   [re-posh.core :as re-posh]
   [oops.core :refer [oget oset!]]))

(def ^:dynamic *dom-node* nil)
(def id-offset 1000000)

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


(defn construct-orbit-controls
  [{:keys [three.orbit-controls/enableDamping
           three.orbit-controls/autoRotate
           three.orbit-controls/zoomSpeed
           three.orbit-controls/minDistance
           three.orbit-controls/maxDistance
           three.orbit-controls/minPolarAngle
           three.orbit-controls/maxPolarAngle]
    :as props}
   dom-node
   camera]
  (assoc props
         :three/obj
         (doto (cond-> (OrbitControls. camera dom-node)
                 enableDamping (oset! "enableDamping" enableDamping)
                 autoRotate    (oset! "autoRotate" autoRotate)
                 zoomSpeed     (oset! "zoomSpeed" zoomSpeed)
                 minDistance   (oset! "minDistance" minDistance)
                 maxDistance   (oset! "maxDistance" maxDistance)
                 minPolarAngle (oset! "minPolarAngle" minPolarAngle)
                 maxPolarAngle (oset! "maxPolarAngle" maxPolarAngle))
           (.addEventListener "end" #(re-posh/dispatch [:three.swig.events/end (+ id-offset (.-id camera))]))
           (.addEventListener "start" #(re-posh/dispatch [:three.swig.events/start (+ id-offset (.-id camera))]))
           (.addEventListener "change" #(re-posh/dispatch [:three.swig.events/change (+ id-offset (.-id camera))])))))

#_(defn construct-event-handlers [obj id handlers]
  (for [{:keys [swig/type three.events/name]} handlers]
    (case type
      :three.events/end    (.addEventListener "end" (fn [t] #_(methods/three-event {:event/name name })))
      :three.events/change (.addEventListener "change" (fn [t])))))

(defmethod construct-scene :swig.type/three.scene
  [{:keys [swig/children
           three/controls]
    :as   props}]
  (let [elems      (map construct-scene children)
        camera-idx (find-elem-idx children :swig.type/three.camera)
        camera     (:three/obj (nth elems camera-idx))
        scene      (three/Scene.)
        dom-node   (doto (.createElement js/document "div")
                     (oset! "style.width" "100%")
                     (oset! "style.height" "100%"))
        canvas     (.appendChild dom-node (.createElement js/document "canvas"))
        renderer   (three/WebGLRenderer. #js {:canvas canvas})]
    (doseq [{:keys [three/obj]} elems]
      (.add scene obj))
    (cond-> props
      true     (assoc :three/obj scene
                      :three/camera camera
                      :three/renderer renderer
                      :three/dom-node dom-node
                      :swig/children elems)
      controls (update :three/controls construct-orbit-controls dom-node camera))))


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
    :or   {position [0 0 0]
           rotation [0 0 0]
           scale    [1.0 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        cam   (three/PerspectiveCamera.)]
    (helpers/set-position! cam position)
    (helpers/set-rotation! cam rotation)
    (helpers/set-scale! cam scale)
    (cond-> cam
      fov    (oset! "fov" fov)
      aspect (oset! "aspect" aspect)
      near   (oset! "near" near)
      far    (oset! "far" far)
      active (oset! "active" active))
    (doseq [{:keys [three/obj]} elems]
      (.add cam obj))
    (assoc props
           :three/obj cam
           :swig/children elems)))


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
           position        [1 1 1]
           rotation        [1 1 1]
           scale           [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        plane (three/Mesh (three/PlaneGeometry. width height width-segments height-segments)
                          (three/MeshBasicMaterial.))]
    (helpers/set-position! plane position)
    (helpers/set-rotation! plane rotation)
    (helpers/set-scale! plane scale)
    (doseq [{:keys [three/obj]} elems]
      (.add plane obj))
    (assoc props
           :three/obj plane
           :swig/children elems)))


(defmethod construct-scene :swig.type/three.object
  [{:keys [three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {position [1 1 1]
           rotation [1 1 1]
           scale    [1 1 1]}
    :as   props}]
  (let [elems  (map construct-scene children)
        object (three/Object3D.)]
    (helpers/set-position! object position)
    (helpers/set-rotation! object rotation)
    (helpers/set-scale! object scale)
    (doseq [{:keys [three/obj]} elems]
      (.add object obj))
    (assoc props
           :three/obj object
           :swig/children elems)))


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
           position        [0 0 0]
           rotation        [0 0 0]
           scale           [1 1 1]
           material        {:color "blue"}}
    :as   props}]
  (let [elems (map construct-scene children)
        mesh  (three/Mesh. (three/SphereGeometry. radius
                                                  width-segments
                                                  height-segments
                                                  phi-start
                                                  phi-length
                                                  theta-start
                                                  theta-length)
                          (three/MeshBasicMaterial. (clj->js material)))]
    (helpers/set-position! mesh position)
    (helpers/set-rotation! mesh rotation)
    (helpers/set-scale! mesh scale)
    (doseq [{:keys [three/obj]} elems]
        (.add mesh obj))
    (assoc props
           :three/obj mesh
           :swig/children elems)))


(defmethod construct-scene :swig.type/three.box
  [{:keys [three.box/width
           three.box/height
           three.box/depth
           three.box/width-segments
           three.box/height-segments
           three.box/depth-segments
           three.object/position
           three.object/rotation
           three.object/scale
           three.object/material
           swig/children]
    :or   {width           1.0
           height          1.0
           depth           1.0
           width-segments  1
           height-segments 1
           depth-segments  1
           position        [0 0 0]
           rotation        [0 0 0]
           scale           [1 1 1]
           material        {:color "red"}}
    :as   props}]
  (let [elems (map construct-scene children)
        box   (three/Mesh. (three/BoxGeometry. width
                                               height
                                               depth
                                               width-segments
                                               height-segments
                                               depth-segments)
                           (three/MeshBasicMaterial. (clj->js material)))]
    (helpers/set-position! box position)
    (helpers/set-rotation! box rotation)
    (helpers/set-scale! box scale)
    (doseq [{:keys [three/obj]} elems]
      (.add box obj))
    (assoc props
           :three/obj box
           :swig/children elems)))


(defn- to-tree [[type props children]]
  (assoc props
         :swig/type type
         :swig/children (map to-tree children)))

(defn entid [entity-name]
  (-> entity-name str hash-string Math/abs))

(defn to-hiccup [{:keys [swig/type swig/children three/obj] :as props}]
  [type
   (dissoc props :swig/children)
   (mapv to-hiccup children)])


(defn to-facts [scene-tree]
  (parser/hiccup->facts scene-tree))


(defn create-scene [tree]
  (-> tree to-tree construct-scene))
