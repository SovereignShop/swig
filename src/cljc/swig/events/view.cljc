(ns swig.events.view
  (:require
   [swig.events.utils :as event-utils]
   [datascript.core :as d]
   #?(:cljs [re-posh.core :as re-posh])
   [swig.macros :as m]))

(m/def-event-ds :swig.events.view/join-views
  [db id]
  (let [tab (event-utils/resolve-operation-target db id)
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

(m/def-event-ds :swig.events.view/divide-view
  [db view-id orientation]
  (let [view (d/entity db view-id)
        parent (event-utils/get-parent view)
        parent-id (:db/id parent)
        new-view-id -1
        new-split-id -2
        view-copy (assoc (event-utils/copy-entity view new-view-id)
                         :swig/index (inc (:swig/index view)))]
    [[:db/retract parent-id :swig.ref/child view-id]
     [:db/add parent-id :swig.ref/child new-split-id]
     {:db/id new-split-id
      :swig.ref/child [view-id view-copy]
      :swig/type :swig.type/split
      :swig.split/orientation orientation
      :swig.split/split-percent 50.1}]))

(m/def-event-ds :swig.events.view/divide-vertical
  [db view-id]
  (divide-view db view-id :vertical))

(m/def-event-ds :swig.events.view/divide-horizontal
  [db view-id]
  (divide-view db view-id :horizontal))

(def get-descendants
  '[[(get-descendants ?parent ?child)
     [?parent :swig.ref/child ?child]]
    [(get-descendants ?parent ?child)
     [?parent :swig.ref/child ?p]
     (get-descendants ?p ?child)]])

#?(:cljs
   (defn dispatch-focus-events [db id]
     (let [unfocus-events
             (d/q
              '[:find ?event-name ?child
                :in $ ?parent %
                :where
                (get-descendants ?parent ?child)
                [?child :swig/event ?event]
                [?event :swig.event/name ?event-name]
                [?event :swig.event/on :swig.event/unfocus]]
              db
              id
              get-descendants)

             focus-events
             (d/q
              '[:find ?event-name ?child
                :in $ ?parent %
                :where
                (get-descendants ?parent ?child)
                [?child :swig/event ?event]
                [?event :swig.event/name ?event-name]
                [?event :swig.event/on :swig.event/focus]]
              db
              id
              get-descendants)]
         (doseq [event (concat unfocus-events focus-events)]
           (re-posh/dispatch event)))))

(m/def-event-ds :swig.events.view/goto-left
  [db view-id]
  (let [view (d/entity db view-id)
        view-id (:db/id view)
        parent (event-utils/get-parent view)

        [target-view-id]
        (d/q
         '[:find [?id (max ?index)]
           :in $ ?parent ?idx
           :where
           [?parent :swig.ref/child ?id]
           [?id :swig/type :swig.type/view]
           [?id :swig/index ?index]
           [(< ?index ?idx)]]
         db
         (:db/id parent)
         (:swig/index view))]
    #?(:cljs
       (dispatch-focus-events db target-view-id))
    (when target-view-id
      [[:db.fn/retractAttribute view-id :swig/has-focus?]
       [:db/add target-view-id :swig/has-focus? true]])))

(m/def-event-ds :swig.events.view/goto-right
  [db view-id]
  (let [view (d/entity db view-id)
        view-id (:db/id view)
        parent (event-utils/get-parent view)

        [target-view-id]
        (d/q
         '[:find [?id (max ?index)]
           :in $ ?parent ?idx
           :where
           [?parent :swig.ref/child ?id]
           [?id :swig/type :swig.type/view]
           [?id :swig/index ?index]
           [(> ?index ?idx)]]
         db
         (:db/id parent)
         (:swig/index view))]
    #?(:cljs
       (dispatch-focus-events db target-view-id))
    (when target-view-id
      [[:db.fn/retractAttribute view-id :swig/has-focus?]
       [:db/add target-view-id :swig/has-focus? true]])))

(comment

  (let [db
        [[5 :follow 3]
         [1 :follow 2]
         [2 :follow 3]
         [3 :follow 4]
         [4 :follow 6]
         [2 :follow 4]]]
    (= (d/q '[:find  ?e2
              :in    $ ?e1 %
              :where (follow ?e1 ?e2)]
            db
            1
            '[[(follow ?e1 ?e2)
               [?e1 :follow ?e2]]
              [(follow ?e1 ?e2)
               [?e1 :follow ?t]
               (follow ?t ?e2)]])
       #{[2] [3] [4] [6]}))

  )
