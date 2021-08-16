(ns swig.events.tab
  (:require
   [swig.events.utils :as event-utils]
   [datascript.core :as d]
   #?(:cljs [swig.macros :refer-macros [def-event-ds]]
      :clj [swig.macros :refer [def-event-ds]])))

(def root-view [:swig/ident :swig/root-view])

(def-event-ds :swig.events.tab/enter-fullscreen
  [db id]
  (let [tab (event-utils/find-ancestor (d/entity db id) :swig.type/tab)
        tab-id (:db/id tab)
        main-view  (d/entity db root-view)
        active-tab (:db/id (:swig.view/active-tab main-view))]
    (into [] #_(event-utils/update-active-tab db tab-id)
          [[:db/retract (:db/id (:swig.ref/parent tab)) :swig.ref/child tab-id]
           {:db/id                    tab-id
            :swig.ref/parent          root-view
            :swig.ref/previous-parent (:db/id (:swig.ref/parent tab))
            :swig.tab/fullscreen      true}
           {:db/id                         root-view
            :swig.ref/child                tab-id
            :swig.view/active-tab          tab-id
            :swig.view/previous-active-tab active-tab}])))

(def-event-ds :swig.events.tab/exit-fullscreen
  [db tab-id]
  (let [tab                 (d/entity db tab-id)
        previous-parent-id  (:db/id (:swig.ref/previous-parent tab))
        main-view           (d/entity db root-view)
        previous-active-tab (:db/id (:swig.view/previous-active-tab main-view))]
    [[:db.fn/retractAttribute tab-id    :swig.tab/fullscreen]
     [:db.fn/retractAttribute tab-id    :swig.ref/previous-parent]
     [:db.fn/retractAttribute root-view :swig.view/previous-active-tab]
     [:db/retract root-view :swig.ref/child tab-id]
     {:db/id           tab-id
      :swig.ref/parent previous-parent-id}
     {:db/id                previous-parent-id
      :swig.ref/child       tab-id
      :swig.view/active-tab tab-id}
     {:db/id                root-view
      :swig.view/active-tab previous-active-tab}]))

(def-event-ds :swig.events.tab/set-active-tab
  [_ view-id tab-id]
  [[:db/add view-id :swig.view/active-tab tab-id]])

(def-event-ds ::move-tab
  [_ tab-id view-id]
  [[:db/add tab-id :swig.ref/parent view-id]])

(def-event-ds :swig.events.tab/divide-tab
  [db id orientation]
  (let [tab (event-utils/find-ancestor (d/entity db id) :swig.type/tab)
        tab-id (:db/id tab)
        view-id
        (d/q '[:find ?view-id .
               :in $ ?tab-id
               :where
               [?tab-id :swig/type :swig.type/tab]
               [?view-id :swig.ref/child ?tab-id]
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
               [?view-id :swig.ref/child ?tab-id]
               [?tab-id :swig/type :swig.type/tab]
               [?view-id :swig/type :swig.type/view]]
             db
             view-id)
        view (d/entity db view-id)
        new-split-id -2
        new-view-id -1]
    (println "view ids:" [view-id new-view-id])
    (println "tab ids:" tab-ids)
    (concat [{:db/id view-parent-id
              :swig.ref/child -2}
             #_[:db/retract view-parent-id :swig.ref/child view-id]
             {:db/id                    new-split-id
              :swig.ref/child           [view-id new-view-id]
              :swig/index               (:swig/index view)
              :swig/type                :swig.type/split
              :swig.ref/parent          view-parent-id
              :swig.split/ops           {:swig/type :swig.type/operations
                                         :db/id -4
                                         :swig.ref/parent new-split-id
                                         :swig.operations/ops
                                         [{:swig/type :swig.operation/re-orient
                                           :swig.ref/parent -4}
                                          {:swig/type :swig.operation/join
                                           :swig.ref/parent -4}]}
              :swig.split/orientation   orientation
              :swig.split/split-percent 50.1}
             {:db/id new-view-id
              :swig.ref/child new-split-id}
             {:db/id                new-view-id
              :swig/index           0
              :swig/type            :swig.type/view
              :swig.ref/parent      new-split-id
              :swig.ref/child        (for [id    tab-ids
                                           :when (not= id tab-id)]
                                       id)
              :swig.view/ops        {:swig/type :swig.type/operations
                                     :db/id -3
                                     :swig.ref/parent new-view-id
                                     :swig.operations/ops
                                     [{:swig/type :swig.operation/divide-vertical
                                       :swig.ref/parent -3}
                                      {:swig/type :swig.operation/divide-horizontal
                                       :swig.ref/parent -3}]}
              :swig.view/active-tab (event-utils/next-tab-id tab-id tab-ids)}
             [:db/add view-id :swig.ref/parent new-split-id]
             [:db/add view-id :swig/index 1]]
            (for [tab-id tab-ids
                   :when (= id tab-id)]
                [:db/retract view-id :swig.ref/child tab-id])
            #_(for [id    tab-ids
                    :when (not= id tab-id)]
                [:db/add -1 :swig.ref/child id]
                #_[:db/add id :swig.ref/parent -1]))))

(def-event-ds :swig.events.tab/delete-tab
  [db tab-id]
  (let [tab (d/entity db tab-id)]
    (conj (event-utils/update-active-tab db tab)
          [:db/retractEntity tab-id])))

(def-event-ds :swig.events.tab/kill-tab
  [db tab-id]
  (let [tab (d/entity db tab-id)]
    (conj (event-utils/update-active-tab db tab)
          [:db.fn/retractAttribute tab-id :swig.ref/parent])))

(def-event-ds :swig.events.tab/duplicate
  [db tab-id]
  (let [tab (d/entity db tab-id)
        children (event-utils/get-children db tab-id)]
    (into [(dissoc (into {} tab) :db/id)]
          (map #(vector % :swig.ref/parent tab-id)
               children))))
