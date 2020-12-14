(ns swig.three.state
  (:require
   [swig.three.helpers :as helpers]
   [datascript.core :as d]
   [swig.parser :as parser]
   [re-posh.core :as re-posh]
   [swig.three.compile :as three-compile]
   [swig.macros :refer-macros [set-attr!]]
   [cljs.core.async :refer [go]]
   [datascript.db :as db :refer [datom-added]]))

(defn update-three! [db f ^int e v]
  (let [ent (d/entity db e)]
    (when-let [obj (:three/obj ent)]
      (f obj v))))

(defn create-scene! [db ^db/Datom d]
  (re-posh/dispatch [:swig.events.core/initialize
                     (-> db
                         (d/entity (.-e d))
                         (parser/facts->hiccup db)
                         (three-compile/create-scene)
                         (three-compile/to-hiccup)
                         (three-compile/to-facts))]))

(defn create-type! [db ^db/Datom d]
  (when-let [obj (:three/obj (:swig.ref/parent (d/entity db (.-e d))))]))

(defn three-listener! [{:keys [tx-data db-after tx-meta]}]
  (when-not (:swig.tx/no-update? tx-meta)
    (doseq [^db/Datom d tx-data]
      (case (.-a d)
        :three.object/position
        (when (datom-added d)
          (update-three! db-after helpers/set-position! (.-e d) (.-v d)))

        :three.object/rotation
        (when (datom-added d)
          (update-three! db-after helpers/set-rotation! (.-e d) (.-v d)))

        :three.object/scale
        (when (datom-added d)
          (update-three! db-after helpers/set-scale! (.-e d) (.-v d)))

        :three.object/visible
        (when (datom-added d)
          (update-three! db-after helpers/set-visible! (.-e d) (.-v d))
          (update-three! db-after helpers/set-visible! (.-e d) false))

        :three.object/up
        (when (datom-added d)
          (update-three! db-after helpers/set-up! (.-e d) (.-v d)))

        :three.object/cast-shadow
        (if (datom-added d)
          (update-three! db-after helpers/set-cast-shadow! (.-e d) (.-v d))
          (update-three! db-after helpers/set-cast-shadow! (.-e d) false))

        :three.object/receive-shadow
        (if (datom-added d)
          (update-three! db-after helpers/set-receive-shadow! (.-e d) (.-v d))
          (update-three! db-after helpers/set-receive-shadow! (.-e d) false))

        :three.shape/arc
        (when (datom-added d)
          (set-attr! db-after "arc" (.-e d) (.-v d)))

        :three.shape/depth
        (when (datom-added d)
          (set-attr! db-after "depth" (.-e d) (.-v d)))

        :three.shape/detail
        (when (datom-added d)
          (set-attr! db-after "detail" (.-e d) (.-v d)))

        :three.shape/height
        (when (datom-added d)
          (set-attr! db-after "height" (.-e d) (.-v d)))

        :three.shape/height-segments
        (when (datom-added d)
          (set-attr! db-after "heightSegments" (.-e d) (.-v d)))

        :three.shape/inner-radius
        (when (datom-added d)
          (set-attr! db-after "innerRadius" (.-e d) (.-v d)))

        :three.shape/material
        (when (datom-added d))

        :three.shape/open-ended?
        (when (datom-added d)
          (set-attr! db-after "openEnded" (.-e d) (.-v d)))

        :three.shape/outer-radius
        (when (datom-added d)
          (set-attr! db-after "outerRadius" (.-e d) (.-v d)))

        :three.shape/p
        (when (datom-added d))

        :three.shape/phi-segments
        (when (datom-added d))

        :three.shape/q
        (when (datom-added d))

        :three.shape/radial-segments
        (when (datom-added d))

        :three.shape/radius
        (when (datom-added d))

        :three.shape/radius-bottom
        (when (datom-added d))

        :three.shape/radius-top
        (when (datom-added d))

        :three.shape/theta-length
        (when (datom-added d))

        :three.shape/theta-segments
        (when (datom-added d))

        :three.shape/theta-start
        (when (datom-added d))

        :three.shape/tube
        (when (datom-added d))

        :three.shape/tubular-segments
        (when (datom-added d))

        :three.light/angle
        (when (datom-added d)
          (set-attr! db-after "angle" (.-e d) (.-v d)))

        :three.light/color
        (when (datom-added d)
          (set-attr! db-after "color" (.-e d) (.-v d)))

        :three.light/decay
        (when (datom-added d)
          (set-attr! db-after "decay" (.-e d) (.-v d)))

        :three.light/distance
        (when (datom-added d)
          (set-attr! db-after "distance" (.-e d) (.-v d)))

        :three.light/ground-color
        (when (datom-added d)
          (set-attr! db-after "groundColor" (.-e d) (.-v d)))

        :three.light/height
        (when (datom-added d)
          (set-attr! db-after "height" (.-e d) (.-v d)))

        :three.light/width
        (when (datom-added d)
          (set-attr! db-after "width" (.-e d) (.-v d)))

        :three.light/intensity
        (when (datom-added d)
          (set-attr! db-after "intensity" (.-e d) (.-v d)))

        :three.light/penumbra
        (when (datom-added d)
          (set-attr! db-after "penumbra" (.-e d) (.-v d)))

        :three.light/power
        (when (datom-added d)
          (set-attr! db-after "power" (.-e d) (.-v d)))

        :three.controls/enable-damping
        (if (datom-added d)
          (set-attr! db-after "enableDamping" (.-e d) (.-v d))
          (set-attr! db-after "enableDamping" (.-e d) false))

        :three.controls/auto-rotate
        (if (datom-added d)
          (set-attr! db-after "autoRotate" (.-e d) (.-v d))
          (set-attr! db-after "autoRotate" (.-e d) false))

        :three.controls/zoom-speed
        (when (datom-added d)
          (set-attr! db-after "zoomSpeed" (.-e d) (.-v d)))

        :three.controls/min-distance
        (when (datom-added d)
          (set-attr! db-after "minDistance" (.-e d) (.-v d)))

        :three.controls/max-distance
        (when (datom-added d)
          (set-attr! db-after "maxDistance" (.-e d) (.-v d)))

        :three.controls/min-polar-angle
        (when (datom-added d)
          (set-attr! db-after "minPolarAngle" (.-e d) (.-v d)))

        :three.controls/max-polar-angle
        (when (datom-added d)
          (set-attr! db-after "maxPolarAngle" (.-e d) (.-v d)))

        :three.controls/enabled
        (if (datom-added d)
          (set-attr! db-after "enabled" (.-e d) (.-v d))
          (set-attr! db-after "enabled" (.-e d) true))

        :three.event.drag/start
        (when (datom-added d))

        :three.event.drag/drag
        (when (datom-added d))

        :three.event.drag/end
        (when (datom-added d))

        :three.material/alpha-map
        (when (datom-added d))

        :three.material/ao-map
        (when (datom-added d))

        :three.material/ao-map-intensity
        (when (datom-added d))

        :three.material/bump-map
        (when (datom-added d))

        :three.material/bump-scale
        (when (datom-added d))

        :three.material/color
        (when (datom-added d))

        :three.material/displacement-map
        (when (datom-added d))

        :three.material/displacement-bias
        (when (datom-added d))

        :three.material/emissive
        (when (datom-added d))

        :three.material/emissive-intensity
        (when (datom-added d))

        :three.material/map
        (when (datom-added d))

        :three.material/morph-normals
        (when (datom-added d))

        :three.material/morph-targets
        (when (datom-added d))

        :three.material/normal-map
        (when (datom-added d))

        :three.material/normal-map-type
        (when (datom-added d))

        :three.material/normal-scale
        (when (datom-added d))

        :three.material/reflectivity
        (when (datom-added d))

        :three.material/refraction-ratio
        (when (datom-added d))

        :three.material/shininess
        (when (datom-added d))

        :three.material/specular
        (when (datom-added d))

        :three.material/specular-map
        (when (datom-added d))

        :three.material/wireframe
        (when (datom-added d))

        :three.material/wireframe-line-cap
        (when (datom-added d))

        :three.material/wireframe-line-join
        (when (datom-added d))

        :three.material/wire-frame-line-width
        (when (datom-added d))

        :three.material/skinning
        (when (datom-added d))

        :three.material/combine
        (when (datom-added d))

        :three.texture/name
        (when (datom-added d))

        :three.texture/image
        (when (datom-added d))

        :three.texture/mipmap
        (when (datom-added d))

        :three.texture/mapping
        (when (datom-added d))

        :three.texture/wrap-s
        (when (datom-added d))

        :three.texture/wrap-t
        (when (datom-added d))

        :three.texture/mag-filter
        (when (datom-added d))

        :three.texture/min-filter
        (when (datom-added d))

        :three.texture/anisotropy
        (when (datom-added d))

        :swig/type
        (case (.-v d)
          :swig.type/three.scene
          (when (datom-added d)
            (create-scene! db-after d))

          :swig.type/three.object
          (if (datom-added d)
            nil
            nil)

          #_:swig.type/three.box
          #_(if (datom-added d)
              nil
              nil)
          nil)

        nil))))


(defn get-editor [db id])


(defn update-editor! [editor {:keys [line column end-line end-column]} new-string]
  (doto editor
    (.setSelection #js {:line line
                        :ch column}
                   #js {:line end-line
                        :ch end-column})
    (.replaceSelection new-string)))

(defn three-selection-range [{:keys [line column end-line end-column]}]
  #js {:anchor #js {:line line :ch column}
       :head #js {:line end-line :ch end-column}})


(defn editor-listener! [{:keys [tx-data db-after tx-meta]}]
  (loop [selections         (array)
         replacements       (array)
         [^db/Datom d & ds] tx-data]
    (if (nil? d)
      [selections replacements]
      (case (.-a d)

        :three.object/position
        (when (datom-added d)
          (when-let [source (meta (.-v d))]
            (recur (doto selections (.push_back (three-selection-range source)))
                   (doto replacements (.push_back (three-selection-range source)))
                   ds)))

        :three.object/rotation
        (when (datom-added d)
          (when-let [source-map (meta (.-v d))]
            (recur (doto selections (.push_back (three-selection-range source-map)))
                   (doto replacements (.push_back (three-selection-range source-map)))
                   ds)))
        nil))))
