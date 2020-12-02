(ns swig.events.tab
  (:require
   [swig.events.utils :as event-utils]
   [datascript.core :as d]
   [swig.macros :as m]))

(def root-view [:swig/ident :swig/root-view])

(m/def-event-ds :swig.events.tab/enter-fullscreen
  [db tab-id]
  (let [tab (d/entity db tab-id)
        main-view  (d/entity db root-view)
        active-tab (:db/id (:swig.view/active-tab main-view))]
    (into (event-utils/update-active-tab db tab-id)
          [{:db/id                    tab-id
            :swig.ref/parent          root-view
            :swig.ref/previous-parent (:db/id (:swig.ref/parent tab))
            :swig.tab/fullscreen      true}
           {:db/id                         root-view
            :swig.view/active-tab          tab-id
            :swig.view/previous-active-tab active-tab}])))

(m/def-event-ds :swig.events.tab/exit-fullscreen
  [db tab-id]
  (let [tab                 (d/entity db tab-id)
        previous-parent-id  (:db/id (:swig.ref/previous-parent tab))
        main-view           (d/entity db root-view)
        previous-active-tab (:db/id (:swig.view/previous-active-tab main-view))]
    [[:db.fn/retractAttribute tab-id    :swig.tab/fullscreen]
     [:db.fn/retractAttribute tab-id    :swig.ref/previous-parent]
     [:db.fn/retractAttribute root-view :swig.view/previous-active-tab]
     {:db/id           tab-id
      :swig.ref/parent previous-parent-id}
     {:db/id                previous-parent-id
      :swig.view/active-tab tab-id}
     {:db/id                root-view
      :swig.view/active-tab previous-active-tab}]))

(m/def-event-ds :swig.events.tab/set-active-tab
  [_ view-id tab-id]
  [[:db/add view-id :swig.view/active-tab tab-id]])

(m/def-event-ds ::move-tab
  [_ tab-id view-id]
  [[:db/add tab-id :swig.ref/parent view-id]])

(m/def-event-ds :swig.events.tab/divide-tab
  [db tab-id orientation]
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
               [?tab-id :swig/type :swig.type/tab]
               [?view-id :swig/type :swig.type/view]]
             db
             view-id)
        view (d/entity db view-id)]
    (concat [{:db/id                    -2
              :swig/index               (:swig/index view)
              :swig/type                :swig.type/split
              :swig.ref/parent          view-parent-id
              :swig.split/ops           {:swig/type :swig.type/operations
                                         :db/id -4
                                         :swig.ref/parent -2
                                         :swig.operations/ops
                                         [{:swig/type :swig.operation/re-orient
                                           :swig.ref/parent -4}
                                          {:swig/type :swig.operation/join
                                           :swig.ref/parent -4}]}
              :swig.split/orientation   orientation
              :swig.split/split-percent 50.1}
             {:db/id                -1
              :swig/index           0
              :swig/type            :swig.type/view
              :swig.ref/parent      -2
              :swig.view/ops        {:swig/type :swig.type/operations
                                     :db/id -3
                                     :swig.ref/parent -1
                                     :swig.operations/ops
                                     [{:swig/type :swig.operation/divide-vertical
                                       :swig.ref/parent -3}
                                      {:swig/type :swig.operation/divide-horizontal
                                       :swig.ref/parent -3}]}
              :swig.view/active-tab (event-utils/next-tab-id tab-id tab-ids)}
             [:db/add view-id :swig.ref/parent -2]
             [:db/add view-id :swig/index 1]]
            (for [id    tab-ids
                  :when (not= id tab-id)]
              [:db/add id :swig.ref/parent -1]))))

(m/def-event-ds :swig.events.tab/delete-tab
  [db tab-id]
  (let [tab (d/entity db tab-id)]
    (conj (event-utils/update-active-tab db tab)
          [:db/retractEntity tab-id])))

(m/def-event-ds :swig.events.tab/kill-tab
  [db tab-id]
  (let [tab (d/entity db tab-id)]
    (conj (event-utils/update-active-tab db tab)
          [:db.fn/retractAttribute tab-id :swig.ref/parent])))
e
