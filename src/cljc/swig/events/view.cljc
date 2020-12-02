(ns swig.events.view
  (:require
   [swig.events.utils :as event-utils]
   [datascript.core :as d]
   [swig.macros :as m]))

(m/def-event-ds ::join-views
  [db id]
  (let [split (event-utils/find-ancestor (d/entity db id) :swig.type/split)
        split-id (:db/id split)
        view-ids
        (d/q '[:find [?view-id ...]
               :in $ ?split-id
               :where
               [?view-id :swig.ref/parent ?split-id]
               [?view-id :swig/type :swig.type/view]
               [?split-id :swig/type :swig.type/split]]
             db
             split-id)
        tab-ids
        (d/q '[:find [?tab-id ...]
               :in $ [?view-id ...]
               :where
               [?tab-id :swig.ref/parent ?view-id]
               [?tab-id :swig/type :swig.type/tab]]
             db
             view-ids)
        parent  (:swig.ref/parent (d/entity db split-id))
        view-id (second view-ids)]
    (concat [[:db/add view-id :swig.ref/parent (:db/id parent)]
             [:db/retractEntity split-id]]
            (for [id    view-ids
                  :when (not= view-id id)]
              [:db/retractEntity id])
            (for [tab-id tab-ids]
              [:db/add tab-id :swig.ref/parent view-id]))))
