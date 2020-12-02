(ns swig.events.utils)

(defn find-ancestor [elem type]
  (let [elem-type (:swig/type elem)]
    (cond (= elem-type type) elem
          (= elem-type :swig/root) (throw #?(:cljs (js/Error. (str "Type not found: " type))
                                             :clj (Exception. (str "Type not found: " type))))
          (nil? elem-type) (throw #?(:clj (Exception.  "nil type!")
                                     :cljs (js/Error. "nil type!")))
          :else (recur (:swig.ref/parent elem) type))))

(defn ancestor-seq [elem]
  (next (iterate :swig.ref/parent elem)))

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
               [?tab-id :swig.ref/parent ?view-id]
               [?tab-id :swig/type :swig.type/tab]]
             db
             (:db/id view))]
    (if (> (count tab-ids) 1)
      (let [new-active-tab (next-tab-id id tab-ids)]
        [[:db/add (:db/id view) :swig.view/active-tab new-active-tab]])
      [])))
