(ns swig.events.view
  (:require
   [swig.events.utils :as event-utils]
   [swig.db :as db]
   [datascript.core :as d]
   #?(:cljs [re-posh.core :as re-posh])
   [swig.macros :as m]))

(def get-descendants
  '[[(get-descendants ?parent ?child)
     [?parent :swig.ref/child ?child]]
    [(get-descendants ?parent ?child)
     [?parent :swig.ref/child ?p]
     (get-descendants ?p ?child)]])

(defn apply-events [db id event]
  (let [events
        (d/q '[:find ?event-name ?child
               :in $ ?parent ?on-event %
               :where
               (get-descendants ?parent ?child)
               [?child :swig/event ?event]
               [?event :swig.event/name ?event-name]
               [?event :swig.event/on ?on-event]]
             db
             id
             event
             get-descendants)]
    (for [[name id] events
          :let [handler (get @db/handlers name)]
          :when handler]
      (handler db id))))

(m/def-event-ds :swig.events.view/delete
  [db view-id]
  (let [view (d/entity db view-id)
        parent (event-utils/get-parent view)
        parent-id (:db/id parent)
        gparent (event-utils/get-parent parent)
        gparent-id (:db/id gparent)
        children (d/q '[:find [?c ...]
                        :in $ ?id ?exclude
                        :where
                        [?id :swig.ref/child ?c]
                        [(not= ?c ?exclude)]]
                      db
                      parent-id
                      view-id)]
    (into [[:db.fn/retractEntity view-id]
           [:db.fn/retractEntity parent-id]]
          (for [c children]
            [:db/add gparent-id :swig.ref/child c]))))

(m/def-event-ds :swig.events.view/close
  [db view-id]
  (let [view (d/entity db view-id)
        parent (event-utils/get-parent view)
        parent-id (:db/id parent)
        gparent (event-utils/get-parent parent)
        gparent-id (:db/id gparent)
        children (d/q '[:find [?c ...]
                        :in $ ?id ?exclude
                        :where
                        [?id :swig.ref/child ?c]
                        [(not= ?c ?exclude)]]
                      db
                      parent-id
                      view-id)]
    (into [[:db/retract parent-id :swig.ref/child view-id]
           [:db.fn/retractEntity parent-id]]
          (for [c children]
            [:db/add gparent-id :swig.ref/child c]))))

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
        new-split-id -1
        new-view-id -2
        view-copy (assoc (event-utils/deep-copy view new-view-id)
                         :swig/index (inc (:swig/index view)))
        {after-divide :db-after tx-data :tx-data tempids :tempids}
        (d/with db
                [[:db/retract parent-id :swig.ref/child view-id]
                 [:db/add parent-id :swig.ref/child new-split-id]
                 {:db/id new-split-id
                  :swig.ref/child [view-id view-copy]
                  :swig/type :swig.type/split
                  :swig.split/orientation orientation
                  :swig.split/split-percent 50.1}])]
    (into tx-data cat (apply-events after-divide (get tempids new-view-id) :swig.event/copy))))

(m/def-event-ds :swig.events.view/divide-vertical
  [db view-id]
  (divide-view db view-id :vertical))

(m/def-event-ds :swig.events.view/divide-horizontal
  [db view-id]
  (divide-view db view-id :horizontal))

(m/def-event-ds :swig.events.view/goto-left
  [db view-id]
  (let [view (d/entity db view-id)
        view-id (:db/id view)

        [target-view-id]
        (reduce (fn [ret x]
                  (if (> (nth x 1) (nth ret 1))
                    x
                    ret))
                (d/q
                 '[:find ?view ?left
                   :in $ ?l ?t
                   :where
                   [?view :swig/type :swig.type/view]
                   [?view :swig.view/left ?left]
                   [?view :swig.view/top ?top]
                   [(< ?left ?l)]
                   [(>= ?top ?t)]]
                 db
                 (:swig.view/left view)
                 (:swig.view/top view)))

        {:keys [db-after tx-data]}
        (d/with db
                [[:db.fn/retractAttribute view-id :swig/has-focus?]
                 [:db/add target-view-id :swig/has-focus? true]])]
    #?(:cljs
       (when target-view-id
         (re-posh/dispatch [:keys.core/update-context {:id target-view-id}])))
    (when target-view-id
      (into tx-data cat (apply-events db-after target-view-id :swig.event/focus)))))

(m/def-event-ds :swig.events.view/goto-right
  [db view-id]
  (let [view (d/entity db view-id)
        view-id (:db/id view)

        [target-view-id]
        (reduce (fn [ret x]
                  (if (< (nth x 1) (nth ret 1))
                    x
                    ret))
                (d/q
                 '[:find ?view ?left
                   :in $ ?l ?t
                   :where
                   [?view :swig/type :swig.type/view]
                   [?view :swig.view/left ?left]
                   [?view :swig.view/top ?top]
                   [(> ?left ?l)]
                   [(>= ?top ?t)]]
                 db
                 (:swig.view/left view)
                 (:swig.view/top view)))
        {:keys [db-after tx-data]}
        (d/with db
                [[:db.fn/retractAttribute view-id :swig/has-focus?]
                 [:db/add target-view-id :swig/has-focus? true]])]
    #?(:cljs
       (when target-view-id
         (re-posh/dispatch [:keys.core/update-context {:id target-view-id}])))
    (when target-view-id
      (into tx-data cat (apply-events db-after target-view-id :swig.event/focus)))))

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
