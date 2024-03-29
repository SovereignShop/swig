(ns swig.three.compile
  (:require
   [goog.dom :as gdom]
   [swig.parser :as parser]
   [three :as three]
   [swig.three.helpers :as helpers]
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
  [{:keys [three.controls/enable-damping
           three.controls/auto-rotate
           three.controls/zoom-speed
           three.controls/min-distance
           three.controls/max-distance
           three.controls/min-polar-angle
           three.controls/max-polar-angle]
    :as   props}
   dom-node
   camera]
  (assoc props
         :three/obj
         (doto (cond-> (OrbitControls. camera dom-node)
                 enable-damping  (oset! "enableDamping" enable-damping)
                 auto-rotate     (oset! "autoRotate" auto-rotate)
                 zoom-speed      (oset! "zoomSpeed" zoom-speed)
                 min-distance    (oset! "minDistance" min-distance)
                 max-distance    (oset! "maxDistance" max-distance)
                 min-polar-angle (oset! "minPolarAngle" min-polar-angle)
                 max-polar-angle (oset! "maxPolarAngle" max-polar-angle))
           #_(.addEventListener "end" #(re-posh/dispatch [:three.swig.events/end (+ id-offset (.-id camera))]))
           #_(.addEventListener "start" #(re-posh/dispatch [:three.swig.events/start (+ id-offset (.-id camera))]))
           #_(.addEventListener "change" #(re-posh/dispatch [:three.swig.events/change (+ id-offset (.-id camera))])))))

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
  [{:keys [three.shape/width
           three.shape/height
           three.shape/depth
           three.shape/width-segments
           three.shape/height-segments
           three.shape/material
           three.object/position
           three.object/rotation
           three.object/scale
           three.object/up
           swig/children]
    :or   {width           1.0
           height          1.0
           depth           1.0
           width-segments  1
           height-segments 1
           depth-segments  1
           position        [1 1 1]
           rotation        [1 1 1]
           scale           [1 1 1]
           up              [0 1 0]
           material        {:color "blue"}}
    :as   props}]
  (let [elems (map construct-scene children)
        plane (three/Mesh. (three/PlaneGeometry. width height width-segments height-segments)
                           (three/MeshBasicMaterial. (clj->js material)))]
    (helpers/set-position! plane position)
    (helpers/set-rotation! plane rotation)
    (helpers/set-scale! plane scale)
    (helpers/set-up! plane up)
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
           #_:three/widgets #_(concat
                           (when-let [frame-id (-> position meta :frame-id)]
                             [{:db/id frame-id}]))
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

(defmethod construct-scene :swig.type/three.cylinder
  [{:keys [three.shape/radius-top
           three.shape/radius-bottom
           three.shape/height
           three.shape/radial-segments
           three.shape/height-segments
           three.shape/open-ended?
           three.shape/theta-start
           three.shape/theta-length
           three.shape/material
           three.object/rotation
           three.object/position
           three.object/scale
           three.object/up
           swig/children]
    :or   {radius-top      1.0
           radius-bottom   1.0
           height          1.0
           radial-segments 8
           height-segments 1
           position        [0 0 0]
           rotation        [0 0 0]
           scale           [1 1 1]
           up              [0 1 0]
           theta-start     0
           theta-length    (* Math/PI 2)}
    :as props}]
  (let [elems (map construct-scene children)
        mesh (three/Mesh. (three/CylinderGeometry. radius-top
                                                   radius-bottom
                                                   height
                                                   radial-segments
                                                   height-segments
                                                   open-ended?
                                                   theta-start
                                                   theta-length)
                          (three/MeshBasicMaterial. (clj->js material)))]
    (helpers/set-position! mesh position)
    (helpers/set-rotation! mesh rotation)
    (helpers/set-scale! mesh scale)
    (helpers/set-up! mesh up)
    (doseq [{:keys [three/obj]} elems]
      (.add mesh obj))
    (assoc props
           :three/obj mesh
           :swig/children elems)))


(defmethod construct-scene :swig.type/three.cone
  [{:keys [three.shape/radius
           three.shape/height
           three.shape/radial-segments
           three.shape/height-segments
           three.shape/open-ended?
           three.shpae/theta-start
           three.shape/theta-length
           three.shape/material
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or {radius 1.0
         height 1.0
         radial-segments 8
         height-segments 1
         theta-start 0
         theta-length (* 2 Math/PI)
         position        [0 0 0]
         rotation        [0 0 0]
         scale           [1 1 1]}
    :as props}]
  (let [elems (map construct-scene children)
        cone (three/Mesh. (three/ConeGeometry. radius height
                                               radial-segments height-segments
                                               open-ended?
                                               theta-start theta-length)
                          (helpers/mesh-phong-material material))]
    (helpers/set-position! cone position)
    (helpers/set-rotation! cone rotation)
    (helpers/set-scale! cone scale)
    (doseq [{:keys [three/obj]} elems]
      (.add cone obj))
    (assoc props
           :three/obj cone
           :swig/children elems)))

(defmethod construct-scene :swig.type/three.dodecahedron
  [{:keys [three.shape/radius
           three.shape/detail
           three.shape/material
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or {radius 1.0
         detail 0.0
         position [0 0 0]
         rotation [0 0 0]
         scale [1 1 1]}
    :as props}]
  (let [elems (map construct-scene children)
        geo (three/Mesh. (three/DodecahedronGeometry. radius detail)
                         (helpers/mesh-phong-material material))]
    (helpers/set-position! geo position)
    (helpers/set-rotation! geo rotation)
    (helpers/set-scale! geo scale)
    (doseq [{:keys [three/obj]} elems]
      (.add geo obj))
    (assoc props
           :three/obj geo
           :swig/children elems)))

(defmethod construct-scene :swig.type/three.icosahedron
  [{:keys [three.shape/radius
           three.shape/detail
           three.shape/material
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or {radius 1.0
         detail 0.0
         position [0 0 0]
         rotation [0 0 0]
         scale [1 1 1]}
    :as props}]
  (let [elems (map construct-scene children)
        geo (three/Mesh. (three/IcosahedronGeometry. radius detail)
                         (helpers/mesh-phong-material material))]
    (helpers/set-position! geo position)
    (helpers/set-rotation! geo rotation)
    (helpers/set-scale! geo scale)
    (doseq [{:keys [three/obj]} elems]
      (.add geo obj))
    (assoc props
           :three/obj geo
           :swig/children elems)))

(defmethod construct-scene :swig.type/three.octahedron
  [{:keys [three.shape/radius
           three.shape/detail
           three.shape/material
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {radius   1.0
           detail   0.0
           position [0 0 0]
           rotation [0 0 0]
           scale    [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        geo   (three/Mesh. (three/OctahedronGeometry. radius detail)
                         (helpers/mesh-phong-material material))]
    (helpers/set-position! geo position)
    (helpers/set-rotation! geo rotation)
    (helpers/set-scale! geo scale)
    (doseq [{:keys [three/obj]} elems]
      (.add geo obj))
    (assoc props
           :three/obj geo
           :swig/children elems)))


(defmethod construct-scene :swig.type/three.ring
  [{:keys [three.shape/inner-radius
           three.shape/outer-radius
           three.shape/theta-segments
           three.shape/phi-segments
           three.shape/theta-start
           three.shape/theta-length
           three.shape/material
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {inner-radius   0.5
           outer-radius   1.0
           theta-segments 8
           phi-segments   8
           theta-start    0
           theta-length   (* 2 Math/PI)
           position       [0 0 0]
           rotation       [0 0 0]
           scale          [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        geo   (three/Mesh. (three/RingGeometry. inner-radius outer-radius
                                              theta-segments phi-segments
                                              theta-start theta-length)
                         (three/MeshBasicMaterial. material))]
    (helpers/set-position! geo position)
    (helpers/set-rotation! geo rotation)
    (helpers/set-scale! geo scale)
    (doseq [{:keys [three/obj]} elems]
      (.add geo obj))
    (assoc props
           :three/obj geo
           :swig/children elems)))


(defmethod construct-scene :swig.type/three.tetrahedron
  [{:keys [three.shape/radius
           three.shape/detail
           three.shape/material
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {radius   1.0
           detail   0.0
           position [0 0 0]
           rotation [0 0 0]
           scale    [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        geo   (three/Mesh. (three/TetrahedronGeometry. radius detail)
                         (three/MeshBasicMaterial. (clj->js material)))]
    (helpers/set-position! geo position)
    (helpers/set-rotation! geo rotation)
    (helpers/set-scale! geo scale)
    (doseq [{:keys [three/obj]} elems]
      (.add geo obj))
    (assoc props
           :three/obj geo
           :swig/children elems)))


(defmethod construct-scene :swig.type/three.torus
  [{:keys [three.shape/radius
           three.shape/tube
           three.shape/radial-segments
           three.shape/tubular-segments
           three.shape/arc
           three.shape/material
           three.object/position
           three.object/rotation
           three.object/scale
           three.object/up
           swig/children]
    :or   {radius           1.0
           tube             0.4
           radial-segments  8
           tubular-segments 6
           arc              (* 2 Math/PI)
           position         [0 0 0]
           rotation         [0 0 0]
           scale            [1 1 1]
           up               [0 1 0]}
    :as   props}]
  (let [elems (map construct-scene children)
        geo   (three/Mesh. (three/TorusGeometry. radius tube radial-segments tubular-segments arc)
                           (three/MeshBasicMaterial. (clj->js material)))]
    (helpers/set-position! geo position)
    (helpers/set-rotation! geo rotation)
    (helpers/set-scale! geo scale)
    (helpers/set-up! geo up)
    (doseq [{:keys [three/obj]} elems]
      (.add geo obj))
    (assoc props
           :three/obj geo
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


(defmethod construct-scene :swig.type/three.torus-knot
  [{:keys [three.shape/radius
           three.shape/tube
           three.shape/tubular-segments
           three.shape/radial-segments
           three.shape/p
           three.shape/q
           three.object/position
           three.object/rotation
           three.object/scale
           three.object/material
           swig/children]
    :or   {radius           1.0
           tube             0.4
           tubular-segments 64
           radial-segments  8
           p                2
           q                3
           position         [0 0 0]
           rotation         [0 0 0]
           scale            [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        geo   (three/Mesh. (three/TorusKnotGeometry. radius tube tubular-segments radial-segments p q)
                           (three/MeshBasicGeometry. (clj->js material)))]
    (helpers/set-position! geo position)
    (helpers/set-rotation! geo rotation)
    (helpers/set-scale! geo scale)
    (doseq [{:keys [three/obj]} elems]
      (.add geo obj))
    (assoc props
           :three/obj geo
           :swig/children elems)))


(defmethod construct-scene :swig.type/three.shape
  [{:keys [three.shape/shape
           three.shape/material
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {position [0 0 0]
           rotation [0 0 0]
           scale    [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        geo   (three/Mesh. (three/ShapeGeometry. shape)
                         (three/MeshBasicGeometry. (clj->js material)))]
    (helpers/set-position! geo position)
    (helpers/set-rotation! geo rotation)
    (helpers/set-scale! geo scale)
    (doseq [{:keys [three/obj]} elems]
      (.add geo obj))
    (assoc props
           :three/obj geo
           :swig/children elems)))


(defmethod construct-scene :swig.type/three.ambient-light
  [{:keys [three.light/color
           three.light/intensity
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {color     0xFFFFFF
           intensity 1.0
           position  [0 0 0]
           rotation  [0 0 0]
           scale     [1 1 1]}
    :as   props}]
  (let [light (three/AmbientLight. color intensity)]
    (helpers/set-position! light position)
    (helpers/set-rotation! light rotation)
    (helpers/set-scale! light scale)
    (assoc props
           :three/obj light
           :three/children children)))


(defmethod construct-scene :swig.type/three.point-light
  [{:keys [three.light/color
           three.light/intensity
           three.light/distance
           three.light/decay
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {color     0xFFFFFF
           intensity 1.0
           distance  0
           decay     1.0
           position  [0 0 0]
           rotation  [0 0 0]
           scale     [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        light (three/PointLight. color intensity distance decay)]
    (helpers/set-position! light position)
    (helpers/set-rotation! light rotation)
    (helpers/set-scale! light scale)
    (assoc props
           :three/obj light
           :three/children elems)))


(defmethod construct-scene :swig.type/three.hemisphere-light
  [{:keys [three.light/sky-color
           three.light/ground-color
           three.light/intensity
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {sky-color    0xFFFFFF
           ground-color 0xFFFFFF
           intensity    1
           position     [0 0 0]
           rotation     [0 0 0]
           scale        [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        light (three/HemisphereLight. sky-color ground-color intensity)]
    (helpers/set-position! light position)
    (helpers/set-rotation! light rotation)
    (helpers/set-scale! light scale)
    (assoc props
           :three/obj light
           :three/children elems)))


(defmethod construct-scene :swig.type/three.directional-light
  [{:keys [three.light/color
           three.light/intensity
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {color     0xFFFFFF
           intensity 1.0
           position  [0 0 0]
           rotation  [0 0 0]
           scale     [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        light (three/DirectionalLight. color intensity)]
    (helpers/set-position! light position)
    (helpers/set-rotation! light rotation)
    (helpers/set-scale! light scale)
    (assoc props
           :three/obj light
           :three/children elems)))


(defmethod construct-scene :swig.type/rect-area-light
  [{:keys [three.light/color
           three.light/intensity
           three.light/width
           three.light/height
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {color     0xFFFFFF
           intensity 1.0
           width     10.0
           height    10.0
           position  [0 0 0]
           rotation  [0 0 0]
           scale     [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        light (three/RectAreaLight. color intensity width height)]
    (helpers/set-position! light position)
    (helpers/set-rotation! light rotation)
    (helpers/set-scale! light scale)
    (assoc props
           :three/obj light
           :three/children elems)))


(defmethod construct-scene :swig.type/splot-light
  [{:keys [three.light/color
           three.light/intensity
           three.light/distance
           three.light/angle
           three.light/penumbra
           three.light/decay
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {color     0xFFFFFF
           intensity 1.0
           distance  0
           angle     (/ Math/PI 2)
           penumbra  0.0
           decay     1.0
           position  [0 0 0]
           rotation  [0 0 0]
           scale     [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        light (three/SpotLight. color intensity distance angle penumbra decay)]
    (helpers/set-position! light position)
    (helpers/set-rotation! light rotation)
    (helpers/set-scale! light scale)
    (assoc props
           :three/obj light
           :three/children elems)))


(defmethod construct-scene :swig.type/three.text
  [{:keys [three.text/text
           three.shape/material
           three.object/position
           three.object/rotation
           three.object/scale
           swig/children]
    :or   {position  [0 0 0]
           rotation  [0 0 0]
           scale     [1 1 1]}
    :as   props}]
  (let [elems (map construct-scene children)
        geo   (helpers/text-geometry text material)]
    (helpers/set-position! geo position)
    (helpers/set-rotation! geo rotation)
    (helpers/set-scale! geo scale)
    (assoc props
           :three/obj geo
           :three/children elems)))


(defn- to-tree [[type props children]]
  (let [m         (meta props)
        form-id   (:form-id m)
        editor-id (:editor-id m)]
    (cond-> (assoc props
                   :swig/type type
                   :swig/children (map to-tree children))
      form-id   (assoc :object/form form-id)
      editor-id (assoc :object/editor editor-id))))


(defn entid [entity-name]
  (-> entity-name str hash-string Math/abs))


(defn to-hiccup [{:keys [swig/type swig/children] :as props}]
  [type
   (dissoc props :swig/children :swig/ident)
   (mapv to-hiccup children)])


(defn to-facts [scene-tree]
  (parser/hiccup->facts scene-tree))


(defn create-scene [tree]
  (-> tree to-tree construct-scene))
