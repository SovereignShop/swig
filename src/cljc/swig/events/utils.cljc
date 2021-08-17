(ns swig.events.utils
  (:require
   [datascript.core :as d]))

(defn get-parent [entity]
  (let [db (d/entity-db entity)
        parent-id (d/q '[:find ?parent . :in $ ?child :where [?parent :swig.ref/child ?child]]
                       db
                       (:db/id entity))]
    (println "parent id:" parent-id)
    (d/entity db parent-id)))

(defn find-operation-entity [db id]
  (let [elem-id (d/q '[:find ?id .
                       :in $ ?op-id
                       :where
                       [?op :swig.operations/ops ?op-id]
                       [?id :swig.element/ops ?op]]
                     db
                     id)]
    (d/entity db elem-id)))

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
