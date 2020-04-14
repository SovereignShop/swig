(ns swig.events
  (:require
   [datascript.core :as d]
   #?(:cljs
      [swig.macros :refer-macros [def-event-ds]])))

(def root-view [:swig/ident :swig/root-view])

;; Generic

#?(:cljs
   (def-event-ds ::initialize [_ [_ facts]] (vec facts)))
#?(:cljs
   (def-event-ds ::debug
     [db [_ id msg pull-pattern]]
     (let [entity (d/pull db (or pull-pattern '[*]) id)]
       (js/console.warn msg " Entity:" entity))))

;; Tab Operations
#?(:cljs
  (def-event-ds ::set-active-tab
    [_ [_ view-id tab-id]]
    [[:db/add view-id :swig.view/active-tab tab-id]]))

(defn next-tab-id [tab-id tab-ids]
  (assert (> (count tab-ids) 1))
  (second (drop-while #(not= % tab-id)
                      (take (inc (count tab-ids))
                            (cycle tab-ids)))))

(defn update-active-tab [db tab]
  (let [view (d/entity db (-> tab :swig.ref/parent :db/id))
        tab-ids
        (d/q '[:find  [?id ...]
               :in $ ?tab-id
               :where
               [?tab-id :swig.ref/parent ?view-id]
               [?id :swig.ref/parent ?view-id]]
             db
             (:db/id tab))]
    (if (> (count tab-ids) 1)
      (let [new-active-tab (next-tab-id (:db/id tab) tab-ids)]
        [[:db/add (:db/id view) :swig.view/active-tab new-active-tab]])
      [])))

#?(:cljs
   (def-event-ds ::enter-fullscreen
     [db [_ tab-id]]
     (let [tab        (d/entity db tab-id)
           view       (d/entity db (-> tab :swig.ref/parent :db/id))
           main-view  (d/entity db root-view)
           active-tab (:db/id (:swig.view/active-tab main-view))]
       (into (update-active-tab db tab)
             [{:db/id                    tab-id
               :swig.ref/parent          root-view
               :swig.ref/previous-parent (:db/id (:swig.ref/parent tab))
               :swig.tab/fullscreen      true}
              {:db/id                         root-view
               :swig.view/active-tab          tab-id
               :swig.view/previous-active-tab active-tab}]))))

#?(:cljs
   (def-event-ds ::exit-fullscreen
     [db [_ tab-id]]
     (let [tab (d/entity db tab-id)
           main-view (d/entity db root-view)]
       [{:db/id tab-id
         :swig.tab/fullscreen false
         :swig.ref/parent (:db/id (:swig.ref/previous-parent tab))}
        {:db/id root-view
         :swig.view/active-tab (:db/id (:swig.view/previous-active-tab main-view))}])))

#?(:cljs
   (def-event-ds ::move-tab
     [db [_ tab-id view-id]]
     [[:db/add tab-id :swig.ref/parent view-id]]))

#?(:cljs
   (def-event-ds ::kill-tab
     [db [_ tab-id]]
     (conj (update-active-tab db (d/entity db tab-id))
           [:db.fn/retractAttribute tab-id :swig.ref/parent])))

#?(:cljs
   (def-event-ds ::delete-tab
     [db [_ tab-id]]
     (conj (update-active-tab db (d/entity db tab-id))
           [:db/retractEntity tab-id])))

(comment) 
#?(:cljs
   (def-event-ds ::divide-tab
     [db [_ tab-id orientation]]
     (let [view-id
           (d/q '[:find ?view-id .
                  :in $ ?tab-id
                  :where
                  [?tab-id :swig/type :swig.type/tab]
                  [?tab-id :swig.ref/parent ?view-id]
                  [?view-id :swig/type :swig.type/view]]
                db
                tab-id)
           view-parent-id
           (d/q '[:find ?parent-id .
                  :in $ ?view-id
                  :where
                  [?view-id :swig.ref/parent ?parent-id]]
                db
                view-id)
           tab-ids
           (d/q '[:find [?tab-id ...]
                  :in $ ?view-id
                  :where
                  [?tab-id :swig.ref/parent ?view-id]
                  [?view-id :swig/type :swig.type/view]]
                db
                view-id)
           view (d/entity db view-id)]
       (concat [{:db/id                    -2
                 :swig/index               (:swig/index view)
                 :swig/type                :swig.type/split
                 :swig.ref/parent          view-parent-id
                 :swig.split/ops           [{:swig/type :swig.type/operation
                                             :swig.operation/name :operation/re-orient}
                                            {:swig/type :swig.type/operation
                                             :swig.operation/name :operation/join}]
                 :swig.split/orientation   orientation
                 :swig.split/split-percent 50.1}
                {:db/id                -1
                 :swig/index           0
                 :swig/type            :swig.type/view
                 :swig.ref/parent      -2
                 :swig.view/ops        [{:swig/type :swig.type/operation
                                         :swig.operation/name :operation/divide-vertical}
                                        {:swig/type :swig.type/operation
                                         :swig.operation/name :operation/divide-horizontal}]
                 :swig.view/active-tab (next-tab-id tab-id tab-ids)}
                [:db/add view-id :swig.ref/parent -2]
                [:db/add view-id :swig/index 1]]
               (for [id    tab-ids
                     :when (not= id tab-id)]
                 [:db/add id :swig.ref/parent -1])))))
(comment 
  #?(:cljs
     (def-event-ds ::partition
       [db [_ ids orientation]]
       (let [elem      (db/entity db id)
             parent    (:swig.ref/parent elem)
             parent-id (:db/id parent)
             siblings  (d/q '[:find [?id ...]
                              :in $ ?parent-id [?included-id ..]
                              [?id :swig.ref/parent ?parent-id]
                              [?id :db/id ?included-id]]
                            db
                            parent-id
                            ids)]
         (concat [{:db/id                    -1
                   :swig/index               (:swig/index view)
                   :swig/type                :swig.type/split
                   :swig.ref/parent          view-parent-id
                   :swig.split/ops           [{:swig/type           :swig.type/operation
                                               :swig.operation/name :operation/re-orient}
                                              {:swig/type           :swig.type/operation
                                               :swig.operation/name :operation/join}]
                   :swig.split/orientation   orientation
                   :swig.split/split-percent 50.1}
                  {:db/id                -2
                   :swig/index           0
                   :swig/type            :swig.type/view
                   :swig.ref/parent      -1
                   :swig.view/ops        [{:swig/type :swig.type/operation
                                           :swig.operation/name :operation/divide-vertical}
                                          {:swig/type :swig.type/operation
                                           :swig.operation/name :operation/divide-horizontal}]}
                  {:db/id                -3
                   :swig/index           0
                   :swig/type            :swig.type/view
                   :swig.ref/parent      -1
                   :swig.view/ops        [{:swig/type :swig.type/operation
                                           :swig.operation/name :operation/divide-vertical}
                                          {:swig/type :swig.type/operation
                                           :swig.operation/name :operation/divide-horizontal}]
                   :swig.view/active-tab (next-tab-id tab-id tab-ids)}]
                 (for [sib-id siblings]
                   [:db/add :swig.ref/parent -1]))))))

(comment) 
#?(:cljs
   (def-event-ds ::join-views
     [db [_ split-id]]
     (let [view-ids
           (d/q '[:find [?view-id ...]
                  :in $ ?split-id
                  :where
                  [?view-id :swig.ref/parent ?split-id]
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
           view-id (first view-ids)]
       (concat [[:db/add view-id :swig.ref/parent (:db/id parent)]
                [:db/retractEntity split-id]]
               (for [id    view-ids
                     :when (not= view-id id)]
                 [:db/retractEntity id])
               (for [tab-id tab-ids]
                 [:db/add tab-id :swig.ref/parent view-id])))))

(comment 
  #?(:cljs
     (def-event-ds ::join-views
       [db [_ id]]
       (let [parent-id (->> id (d/entity db) :swig.ref/parent :db/id)
             child-ids
             (d/q '[:find [?child-id ...]
                    :in $ ?parent-id
                    :where
                    [?child-id :swig.ref/parent ?parent-id]]
                  db
                  id)]
         (cons [:db.fn/retractAttribute id :swig.ref/parent]
               (for [child-id child-ids]
                 [:db/add child-id :swig.ref/parent parent-id]))))))

#?(:cljs
   (def-event-ds ::set-split-percent
     [_ [_ split-id percent]]
     [[:db/add split-id :swig.split/split-percent (+ (float percent)
                                                     0.001)]]))
