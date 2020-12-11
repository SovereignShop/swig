(ns swig.three.state
  (:require
   [swig.parser :as parser]
   [swig.three.helpers :as helpers]
   [swig.three.compile :as tc]
   [datascript.core :as d]
   [datascript.db :as db :refer [datom-added]]))

(defn update-three! [db f ^int e v]
  (let [ent (-> db (d/entity e))        ; TODO: cache entities
        obj (:three/obj ent)]
    (f obj v)))

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

        nil))))
