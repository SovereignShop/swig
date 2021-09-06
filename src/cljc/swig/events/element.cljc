(ns swig.events.element
  #?(:cljs
     (:require-macros
      [swig.macros :refer [def-event-ds defevent]]))
  (:require
   [swig.events.utils :as event-utils]
   [swig.db :as db]
   [datascript.core :as d]
   #?(:clj [swig.macros :refer [def-event-ds defevent]])))

(def context-ident
  [:swig/ident :swig.ident/context])

(def-event-ds ::set-context
  [_ id]
  [{:swig/ident :swig.ident/context
    :swig.context/id id}])

(def root-view [:swig/ident :swig/root-view])

(defn get-context-id [db]
  (:swig.context/id (d/entity db context-ident)))

(def get-descendants
  '[[(get-descendants ?parent ?child)
     [?parent :swig.ref/child ?child]]
    [(get-descendants ?parent ?child)
     [?parent :swig.ref/child ?p]
     (get-descendants ?p ?child)]])

(def get-ancestors
  '[[(get-ancestors ?gp ?p)
     [?gp :swig.ref/child ?p]]
    [(get-ancestors ?gp ?p)
     [?ggp :swig.ref/child ?gp]
     (get-ancestors ?ggp ?p)]])

(defn apply-swig-events [db id event]
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

(def-event-ds ::maximize
  ([db] (maximize db (get-context-id db)))
  ([db id]
   (let [max-elem-id
         (d/q '[:find ?max .
                :in $ ?c %
                :where
                (get-ancestors ?id ?c)
                [?id :swig.element/maximized-element ?max]]
              db
              id
              get-ancestors)]
     [[:db/add (or max-elem-id root-view) :swig.element/maximized-element id]])))

(def-event-ds ::unmaximize
  ([db] (unmaximize db (get-context-id db)))
  ([db id]
   (let [elem-id
         (d/q '[:find ?id .
                :in $ ?c %
                :where
                (get-ancestors ?c ?id)
                [?id :swig.element/maximized-element]]
              db
              id)]
     [[:db.fn/retractAttribute elem-id :swig.element/maximized-element]])))

(def-event-ds ::toggle-maximize
  ([db] (toggle-maximize db (get-context-id db)))
  ([db id]
   (if-let [parent-id (d/q '[:find ?p .
                             :in $ ?id
                             :where
                             [?p :swig.element/maximized-element ?id]]
                           db
                           id)]
     [[:db.fn/retractAttribute parent-id :swig.element/maximized-element]]
     (maximize db id))))

(def-event-ds ::go-left
  ([db] (go-left db (get-context-id db)))
  ([db id]
   (let [element (d/entity db id)
         element-id (:db/id element)

         [target-element-id]
         (reduce (fn [ret x]
                   (if (> (nth x 1) (nth ret 1))
                     x
                     ret))
                 (d/q
                  '[:find ?element ?left
                    :in $ ?l ?t
                    :where
                    [?element :swig/type :swig.type/element]
                    [?element :swig.element/left ?left]
                    [?element :swig.element/top ?top]
                    [(< ?left ?l)]
                    [(>= ?top ?t)]]
                  db
                  (:swig.element/left element)
                  (:swig.element/top element)))

         {:keys [db-after tx-data]}
         (d/with db
                 [[:db.fn/retractAttribute element-id :swig/has-focus?]
                  [:db/add target-element-id :swig/has-focus? true]])]
     (when target-element-id
       (into tx-data cat (apply-swig-events db-after target-element-id :swig.event/focus))))))

(def-event-ds ::close
  ([db] (close db (get-context-id db)))
  ([db element-id]
   (let [element (d/entity db element-id)
         parent (event-utils/get-parent element)
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
                       element-id)]
     (into [[:db/retract parent-id :swig.ref/child element-id]
            [:db.fn/retractEntity parent-id]]
           (for [c children]
             [:db/add gparent-id :swig.ref/child c])))))

(def-event-ds ::delete
  ([db] (delete db (get-context-id db)))
  ([db element-id]
   (let [element (d/entity db element-id)
        parent (event-utils/get-parent element)
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
                      element-id)]
    (into [[:db.fn/retractEntity element-id]
           [:db.fn/retractEntity parent-id]]
          (for [c children]
            [:db/add gparent-id :swig.ref/child c])))))

(defn- divide-impl
  ([db element-id orientation]
  (let [element (d/entity db element-id)
        parent (event-utils/get-parent element)
        parent-id (:db/id parent)
        new-split-id -1
        new-element-id -2
        element-copy (assoc (event-utils/deep-copy element new-element-id)
                            :swig/index (inc (:swig/index element)))
        {after-divide :db-after tx-data :tx-data tempids :tempids}
        (d/with db
                [[:db/retract parent-id :swig.ref/child element-id]
                 [:db/add parent-id :swig.ref/child new-split-id]
                 {:db/id new-split-id
                  :swig.ref/child [element-id element-copy]
                  :swig/type :swig.type/split
                  :swig.split/orientation orientation
                  :swig.split/split-percent 50.1}])]
    (into tx-data cat (apply-swig-events after-divide (get tempids new-element-id) :swig.event/copy)))))

(def-event-ds ::divide-vertical
  ([db] (divide-vertical db (get-context-id db)))
  ([db element-id]
   (divide-impl db element-id :vertical)))

(def-event-ds ::divide-horizontal
  ([db] (divide-horizontal db (get-context-id db)))
  ([db element-id]
   (divide-impl db element-id :horizontal)))
