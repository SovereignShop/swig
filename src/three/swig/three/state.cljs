(ns swig.three.state
  (:require
   [swig.three.helpers :as helpers]
   [datascript.core :as d]
   [swig.parser :as parser]
   [re-posh.core :as re-posh]
   [swig.three.compile :as three-compile]
   [cljs.core.async :refer [go]]
   [datascript.db :as db :refer [datom-added]]))

(defn update-three! [db f ^int e v]
  (let [ent (d/entity db e)]
    (when-let [obj (:three/obj ent)]
      (f obj v))))

(defn create-scene! [db ^db/Datom d]
  (let [scene-entity (d/entity db (.-e d))
        tree (parser/facts->hiccup scene-entity db)]
    (go
      (let [facts (-> tree
                      (three-compile/create-scene)
                      (three-compile/to-hiccup)
                      (three-compile/to-facts))]
        (re-posh/dispatch [:swig.events.core/initialize facts])))))

(defn create-type! [ db ^db/Datom d]
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

        :three.object/cast-shadow
        (if (datom-added d)
          (update-three! db-after helpers/set-cast-shadow! (.-e d) (.-v d))
          (update-three! db-after helpers/set-cast-shadow! (.-e d) false))

        :three.object/receive-shadow
        (if (datom-added d)
          (update-three! db-after helpers/set-receive-shadow! (.-e d) (.-v d))
          (update-three! db-after helpers/set-receive-shadow! (.-e d) false))

        :swig/type
        (case (.-v d)
          :swig.type/three.scene
          (when (datom-added d)
            (create-scene! db-after d))

          #_:swig.type/three.object
          #_(if (datom-added d)
              nil
              nil)

          #_:swig.type/three.box
          #_(if (datom-added d)
              nil
              nil)
          nil)

        nil))))
