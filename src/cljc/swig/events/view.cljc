(ns swig.events.view
  (:require
   [swig.events.utils :as event-utils]
   [datascript.core :as d]
   [swig.macros :as m]))

(m/def-event-ds :swig.events.view/join-views
  [db id]
  (let [tab (event-utils/find-operation-entity db id)
        split (event-utils/find-ancestor tab :swig.type/split)
        split-id (:db/id split)
        view-ids
        (d/q '[:find [?view-id ...]
               :in $ ?split-id
               :where
               [?split-id :swig.ref/child ?view-id]
               [?view-id :swig/type :swig.type/view]
               [?split-id :swig/type :swig.type/split]]
             db
             split-id)
        tab-ids
        (d/q '[:find [?tab-id ...]
               :in $ [?view-id ...]
               :where
               [?view-id :swig.ref/child ?tab-id]
               [?tab-id :swig/type :swig.type/tab]]
             db
             view-ids)
        parent  (event-utils/get-parent (d/entity db split-id))
        view-id (second view-ids)]
    (concat [[:db/add (:db/id parent) :swig.ref/child view-id]
             [:db/retractEntity split-id]]
            (for [id    view-ids
                  :when (not= view-id id)]
              [:db/retractEntity id])
            (for [tab-id tab-ids]
              [:db/add view-id :swig.ref/child tab-id]))))
