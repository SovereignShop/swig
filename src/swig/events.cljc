(ns swig.events
  (:require
   [datascript.core :as d]
   #?(:cljs [re-posh.core :as re-posh])))

(def root-view [:swig/ident :swig/root-view])

(defn find-ancestor [elem type]
  (let [elem-type (:swig/type elem)]
    (cond (= elem-type type) parent
          (= elem-type :swig/root) (throw (js/Error. (str "Type not found: " type)))
          :else (recur (:swig.ref/parent elem) type))))

(defn next-tab-id [tab-id tab-ids]
  (assert (> (count tab-ids) 1))
  (second (drop-while #(not= % tab-id)
                      (take (inc (count tab-ids))
                            (cycle tab-ids)))))

(defn update-active-tab [db id]
  (let [view (find-ancestor (d/entity db id) :swig.type/view)
        tab-ids
        (d/q '[:find  [?tab-id ...]
               :in $ ?view-id
               :where
               [?tab-id :swig.ref/parent ?view-id]]
             db
             (:db/id view))]
    (if (> (count tab-ids) 1)
      (let [new-active-tab (next-tab-id (:db/id tab) tab-ids)]
        [[:db/add (:db/id view) :swig.view/active-tab new-active-tab]])
      [])))

(defn enter-fullscreen [db tab]
  (let [main-view  (d/entity db root-view)
        active-tab (:db/id (:swig.view/active-tab main-view))]
    (into (update-active-tab db (:db/id tab))
          [{:db/id                    tab-id
            :swig.ref/parent          root-view
            :swig.ref/previous-parent (:db/id (:swig.ref/parent tab))
            :swig.tab/fullscreen      true}
           {:db/id                         root-view
            :swig.view/active-tab          tab-id
            :swig.view/previous-active-tab active-tab}])))

(defn exit-fullscreen [db tab]
  (let [main-view (d/entity db root-view)]
    [{:db/id (:db/id tab)
      :swig.tab/fullscreen false
      :swig.ref/parent (:db/id (:swig.ref/previous-parent tab))}
     {:db/id root-view
      :swig.view/active-tab (:db/id (:swig.view/previous-active-tab main-view))}]))

(defn move-tab [tab-id view-id]
  [[:db/add tab-id :swig.ref/parent view-id]])

(defn kill-tab [db tab]
  (conj (update-active-tab db tab)
        [:db.fn/retractAttribute (:db/id tab) :swig.ref/parent]))

(defn delete-tab [db tab]
  (conj (update-active-tab db tab)
        [:db/retractEntity (:db/id tab)]))

(defn divide-tab
  "Split a view into a split with two views as children. The new
  split contains two view children."
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
              [:db/add id :swig.ref/parent -1]))))

(defn join-views
  "Join the two view children of a split into a single view."
  [db split]
  (let [split
        view-ids
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
              [:db/add tab-id :swig.ref/parent view-id]))))

(defn set-split-percent [split-id percent]
  [[:db/add split-id :swig.split/split-percent (+ (float percent) 0.001)]])

#?(:cljs
   (re-posh/reg-event-ds
    ::enter-fullscreen
    (fn reg-enter-fullscreen
      [db [_ id]]
      (let [tab (find-ancestor (d/entity db id) ancestor-type)]
        (enter-fullscreen db tab)))))

#?(:cljs
   (re-posh/reg-event-ds
    ::exit-fullscreen
    (fn reg-exit-fullscreen
      [db [_ id]]
      (let [tab (find-ancestor (d/entity db id) :swig.type/tab)]
        (exit-fullscreen db tab)))))

#?(:cljs
   (re-posh/reg-event-ds
    ::move-tab
    (fn move-tab
      [db [_ tab-id view-id]]
      (move-tab db tab-id view-ids))))

#?(:cljs
   (re-posh/reg-event-ds
    ::kill-tab
    (fn reg-kill-tab
      [db [_ id]]
      (let [tab (find-ancestor (d/entity db id) :swig.type/tab)]
        (kill-tab db id)))))

#?(:cljs
   (re-posh/reg-event-ds
    ::delete-tab
    (fn reg-delete-tab
      [db [_ id]]
      (let [tab (find-ancestor (d/entity db id) :swig.type/tab)]
        (delete-tab db tab)))))

#?(:cljs
   (re-posh/reg-event-ds
    ::divide-tab
    (fn reg-divide-tab
      [db [_ id orientation]]
      (let [tab-id (:db/id (find-ancestor (d/entity db id) :swig.type/tab))]
        (divide-tab db tab-id orientation)))))

#?(:cljs
   (re-posh/reg-event-ds
    ::join-views
    (fn reg-join-views
      [db [_ id]]
      (let [split (find-ancestor (d/entity db id) :swit.type/split)]
        (join-views db split)))))

#?(:cljs
   (re-posh/reg-event-ds
    ::set-split-percent
    (fn reg-set-split-percent
      [db [_ split-id split-percent]]
      (set-split-percent split-id split-percent))))
