(ns swig.events.element
  #?(:cljs
     (:require-macros
      [swig.macros :refer [def-event-ds defevent]]))
  (:require
   [swig.events.rules :refer [get-ancestors get-descendants]]
   [swig.events.utils :as eu :refer [context-ident get-context-id]]
   [swig.db :as db]
   [datascript.core :as d]
   #?(:clj [swig.macros :refer [def-event-ds defevent]])))

(def-event-ds ::set-context
  [_ id]
  [{:swig/ident :swig.ident/context
    :swig.context/id id}])

(def root-view [:swig/ident :swig/root-view])

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
         (d/q '[:find ?id .
                :in $ ?c %
                :where
                (get-ancestors ?id ?c)
                [?max :swig.element/maximized-element ?id]]
              db
              id
              get-ancestors)
         parent-id (:db/id (eu/get-parent (d/entity db id)))]
     [[:db/add (or max-elem-id root-view) :swig.element/maximized-element id]
      [:db/retract parent-id :swig.ref/child id]
      [:db/add id :swig.ref/previous-parent parent-id]
      [:db/add (-> (d/entity db id) :swig.ref/child first :db/id) :swig.element/maximized true]])))

(def-event-ds ::toggle-maximize
  ([db] (toggle-maximize db (get-context-id db)))
  ([db id]
   (if-let [parent-id (d/q '[:find ?p .
                             :in $ ?id
                             :where
                             [?p :swig.element/maximized-element ?id]]
                           db
                           id)]
     [[:db.fn/retractAttribute parent-id :swig.element/maximized-element]
      [:db.fn/retractAttribute id :swig.ref/previous-parent]
      [:db/add (-> (d/entity db id) :swig.ref/previous-parent :db/id) :swig.ref/child id]]
     (maximize db id))))

(defn- get-tab-sizes []
  (for [e (.getElementsByClassName js/document "swig-leaf")]
    {:id (long (.getAttribute e "swigid"))
     :left (.-offsetLeft e)
     :top (.-offsetTop e)
     :width (.-offsetWidth e)
     :height (.-offsetHeight e)}))

(defn- find-nearest-left-tab [id]
  (let [tab-element (.getElementById js/document (str "swig-" id))
        l (.-offsetLeft tab-element)
        t (.-offsetTop tab-element)]
    (loop [nearest nil
           [{:keys [left top] :as tab} & tabs] (get-tab-sizes)]
      (cond (nil? tab) nearest
            (< left l) (if (or (nil? nearest) (> left (:left nearest)))
                         (recur tab tabs)
                         (recur nearest tabs))
            :else
            (recur nearest tabs)))))

(defn- find-nearest-right-tab [id]
  (let [tab-element (.getElementById js/document (str "swig-" id))
        l (.-offsetLeft tab-element)
        t (.-offsetTop tab-element)]
    (loop [nearest nil
           [{:keys [left top] :as tab} & tabs] (get-tab-sizes)]
      (cond (nil? tab) nearest
            (> left l) (if (or (nil? nearest) (< left (:left nearest)))
                         (recur tab tabs)
                         (recur nearest tabs))
            :else
            (recur nearest tabs)))))

(def-event-ds ::go-left
  ([db]
   (go-left db (get-context-id db)))
  ([db element-id]
   (when-let [target-element-child-id (:id (find-nearest-left-tab element-id))]
     (let [target-element-id (:db/id
                              (eu/get-parent
                               (d/entity db target-element-child-id)))
           {:keys [db-after tx-data]}
           (d/with db
                   [[:db.fn/retractAttribute element-id :swig/has-focus?]
                    [:db/add target-element-id :swig/has-focus? true]
                    [:db/add context-ident :swig.context/id target-element-id]])]
       (into tx-data cat (apply-swig-events db-after target-element-id :swig.event/focus))))))

(def-event-ds ::go-right
  ([db]
   (go-right db (get-context-id db)))
  ([db element-id]
   (when-let [target-element-child-id (:id (find-nearest-right-tab element-id))]
     (let [target-element-id (:db/id
                              (eu/get-parent
                               (d/entity db target-element-child-id)))
           {:keys [db-after tx-data]}
           (d/with db
                   [[:db.fn/retractAttribute element-id :swig/has-focus?]
                    [:db/add target-element-id :swig/has-focus? true]
                    [:db/add context-ident :swig.context/id target-element-id]])]
       (into tx-data cat (apply-swig-events db-after target-element-id :swig.event/focus))))))

(defn- focus-on [db id]
  (into [[:db/add context-ident :swig.context/id id]]
        cat
        (apply-swig-events db id :swig.event/focus)))

(def-event-ds ::close
  ([db] (close db (get-context-id db)))
  ([db element-id]
   (let [element (d/entity db element-id)
         parent (eu/get-parent element)
         parent-id (:db/id parent)
         gparent (eu/get-parent parent)
         gparent-id (:db/id gparent)
         children (d/q '[:find [?c ...]
                         :in $ ?id ?exclude
                         :where
                         [?id :swig.ref/child ?c]
                         [(not= ?c ?exclude)]]
                       db
                       parent-id
                       element-id)]
     (concat
      [[:db/retract parent-id :swig.ref/child element-id]
       [:db.fn/retractEntity parent-id]]
      (focus-on db (first children))
      (for [c children]
        [:db/add gparent-id :swig.ref/child c])))))

(def-event-ds ::delete
  ([db] (delete db (get-context-id db)))
  ([db element-id]
   (let [element (d/entity db element-id)
        parent (eu/get-parent element)
        parent-id (:db/id parent)
        gparent (eu/get-parent parent)
        gparent-id (:db/id gparent)
        children (d/q '[:find [?c ...]
                        :in $ ?id ?exclude
                        :where
                        [?id :swig.ref/child ?c]
                        [(not= ?c ?exclude)]]
                      db
                      parent-id
                      element-id)]
     (concat
      [[:db.fn/retractEntity element-id]
       [:db.fn/retractEntity parent-id]]
      (focus-on db (first children))
      (for [c children]
        [:db/add gparent-id :swig.ref/child c])))))

(defn- swap-references [db old-id new-id]
  (let [refs (d/q '[:find ?i ?attr
                    :in $ ?id
                    :where
                    [?i ?attr ?id]]
                  db
                  old-id)]
    (for [[id attr] refs
          fact [[:db/retract id attr old-id]
                [:db/add id attr new-id]]]
      fact)))

(defn- divide-impl
  ([db element-id orientation]
   (let [element (d/entity db element-id)
         parent (eu/get-parent element)
         new-split-id -1
         new-element-id -2
         element-copy (eu/deep-copy element new-element-id)
         {after-divide :db-after tx-data :tx-data tempids :tempids}
         (d/with db
                 (into (swap-references db element-id new-split-id)
                       [{:db/id new-split-id
                         :swig/index (:swig/index element)
                         :swig.ref/child [element-id element-copy]
                         :swig/type :swig.type/split
                         :swig.split/orientation orientation
                         :swig.split/split-percent 50.1}
                        [:db/add element-id :swig/index 0]
                        [:db/add new-element-id :swig/index 1]]))]
     (into (conj tx-data [:db/add context-ident :swig.context/id (get tempids new-element-id)])
           cat
           (apply-swig-events after-divide (get tempids new-element-id) :swig.event/copy)))))

(def-event-ds ::divide-vertical
  ([db] (divide-vertical db (get-context-id db)))
  ([db element-id]
   (divide-impl db element-id :vertical)))

(def-event-ds ::divide-horizontal
  ([db] (divide-horizontal db (get-context-id db)))
  ([db element-id]
   (divide-impl db element-id :horizontal)))
