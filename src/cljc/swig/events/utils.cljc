(ns swig.events.utils
  (:require
   [datascript.db :as db]
   [datascript.core :as d]))

(def context-ident
  [:swig/ident :swig.ident/context])

(defn get-context-id [db]
  (:swig.context/id (d/entity db context-ident)))

(defn get-parent [entity]
  (let [db (d/entity-db entity)
        parent-id (d/q '[:find ?parent . :in $ ?child :where [?parent :swig.ref/child ?child]]
                       db
                       (:db/id entity))]
    (d/entity db parent-id)))

(defn copy-entity [entity new-id]
  (-> (into {} entity)
      (assoc  :db/id new-id)
      (dissoc :swig/ident)
      (update :swig.ref/child (partial mapv :db/id))
      (update :swig.view/active-tab :db/id)))

(defn resolve-operation-target [db id]
  (let [entity (d/entity db id)
        entity-type (:swig/type entity)]
    (case entity-type
      (:swig.type/view :swig.type/tab :swig.type/split)
      entity

      (d/entity db
                (d/q '[:find ?id .
                       :in $ ?op-id
                       :where
                       [?op :swig.operations/ops ?op-id]
                       [?id :swig.element/ops ?op]]
                     db
                     id)))))

(defn find-ancestor [elem type]
  (let [elem-type (:swig/type elem)]
    (cond (= elem-type type) elem
          (= elem-type :swig/root) (throw #?(:cljs (js/Error. (str "Type not found: " type))
                                             :clj (Exception. (str "Type not found: " type))))
          (nil? elem-type) (throw #?(:clj (Exception.  "nil type!")
                                     :cljs (js/Error. "nil type!")))
          :else (recur (get-parent elem) type))))

(defn ancestor-seq [elem]
  (next (iterate get-parent elem)))

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
               [?view-id :swig.ref/child ?tab-id]
               [?tab-id :swig/type :swig.type/tab]]
             db
             (:db/id view))]
    (if (> (count tab-ids) 1)
      (let [new-active-tab (next-tab-id id tab-ids)]
        [[:db/add (:db/id view) :swig.view/active-tab new-active-tab]])
      [])))

(defn get-children [db id]
  (d/q '[:find (pull ?child-id [*])
         :in $ ?id
         :where
         [?id :swig.ref/child ?child-id]]))

(def get-refs
  (memoize
   (fn [schema]
     (let [refs (into {}
                      (filter (fn [[k v]] (= (:db/valueType v) :db.type/ref)))
                      schema)]
       refs))))

(defn walk-refs [entity]
  (let [db (d/entity-db entity)
        ref-idents (get-refs (db/-schema db))]
    (cons entity
          (for [[k v] (seq entity)
                :let [s (k ref-idents)]
                :when s
                ret (let [card (:db/cardinality s)]
                      (if (= card :db.cardinality/many)
                        (mapcat walk-refs v)
                        (walk-refs v)))]
            ret))))

(defn deep-copy
  ([entity]
   (deep-copy entity -1))
  ([entity id]
   (let [db (d/entity-db entity)
         schema (db/-schema db)
         ids (atom (inc id))
         id-mapping (atom {})
         walk (fn walk [e]
                (if-let [new-id (get @id-mapping (:db/id e))]
                  new-id
                  (into {:db/id (let [new-id (swap! ids dec)]
                                  (swap! id-mapping assoc (:db/id e) new-id)
                                  new-id)}
                        (comp
                         (remove (fn [x] (= (key x) :swig/ident)))
                         (map (fn [[k v :as pair]]
                                (let [s (k schema)]
                                  (if (= (:db/valueType s) :db.type/ref)
                                    (if (= (:db/cardinality s) :db.cardinality/many)
                                      [k (mapv walk v)]
                                      [k (walk v)])
                                    pair)))))
                        e)))]
     (walk entity))))
