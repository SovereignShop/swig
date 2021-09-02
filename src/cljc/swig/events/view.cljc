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


(def get-descendants
  '[[(get-descendants ?parent ?child)
     [?parent :swig.ref/child ?child]]
    [(get-descendants ?parent ?child)
     [?parent :swig.ref/child ?p]
     (get-descendants ?p ?child)]])


#?(:cljs
   (defn dispatch-copy-events [db id]
     (let [copy-entity-events
           (d/q '[:find ?event-name ?child
                  :in $ parent %
                  :where
                  (get-descendants ?parent ?child)
                  [?child :swig/event ?event]
                  [?event :swig.event/name ?event-name]
                  [?event :swig.event/on :swig.event/copy]]
                db
                id
                get-descendants)]
       (println "events:" copy-entity-events)
       (doseq [event copy-entity-events]
         (do (println "sending event:" event)
             (re-posh/dispatch event))))))

(defn copy-editor [db id]
  (let [editor (d/entity db id)
        parent (event-utils/get-parent editor)
        parent-id (:db/id parent)
        document (:editor/document editor)]
    [[:db/retract parent-id :swig.ref/child id]
     (-> (into {} editor)
         (assoc
          :db/id -1
          :editor/document
          (-> (into {} document)
              (assoc
               :db/id -2
               :editor.doc/linked-to (:db/id document))
              (update :editor.doc/cursor :db/id)))
         (update :swig/event #(mapv :db/id %))
         (dissoc :cursor/token))]))

(m/def-event-ds :swig.events.view/divide-view
  [db view-id orientation]
  (let [view (d/entity db view-id)
        parent (event-utils/get-parent view)
        parent-id (:db/id parent)
        new-split-id -1
        view-copy (assoc (event-utils/deep-copy view -2)
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
                 (:swig.view/top view)))]
    #?(:cljs
       (when target-view-id
         (dispatch-focus-events db target-view-id)
         (re-posh/dispatch [:keys.core/update-context {:id target-view-id}])))
    (when target-view-id
      [[:db.fn/retractAttribute view-id :swig/has-focus?]
       [:db/add target-view-id :swig/has-focus? true]])))

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
                 (:swig.view/top view)))]
    #?(:cljs
       (when target-view-id
         (dispatch-focus-events db target-view-id)
         (re-posh/dispatch [:keys.core/update-context {:id target-view-id}])))
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
