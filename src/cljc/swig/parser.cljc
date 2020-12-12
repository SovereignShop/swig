(ns swig.parser
  (:require
   #?(:cljs [cljs.spec.alpha :as s]
      :clj [clojure.spec.alpha :as s])
   [malli.core :as m]
   [swig.spec :as spec]
   [datascript.core :as d]
   #?(:clj [clojure.core.match :refer [match]]
      :cljs [cljs.core.match :refer-macros [match]])))

(def root-view [:swig/ident :swig/root-view])

(def non-closing-elements
  #{:area :base :br :col :command
    :embed :hr :img :input :link
    :meta :keygen :param :source
    :track :wbr})

(def element-names #{:swig.type/view :swig.type/split :swig.type/tab})

(def tab-ops #{:swig.ops.tabs/divide-horizontal
               :swig.ops.tabs/divide-vertical
               :swig.ops.tabs/fullscreen})

(s/def ::params (s/map-of keyword? any?))

(s/def ::element
  (s/cat :name keyword?
         :args (s/? ::params)
         :body (s/or :nodes (s/* ::node)
                     :nodes-list (s/coll-of ::node))))

(s/def ::empty-element
  (s/cat :name keyword?
         :args (s/? ::params)))

(s/def ::node
  (s/or :empty-element ::empty-element
        :element ::element))

(def internal-keys #{:swig/index :swig.ref/parent :swig/type})

(defmulti compile-hiccup-impl (fn [elem _ _] (:swig/type elem)))

(defmethod compile-hiccup-impl :default
  [props _ _]
  props)

(defmethod compile-hiccup-impl :swig.type/tab
  [{id :db/id :as props} id-gen _]
  (cond-> props
    (:swig.tab/ops props)
    (update :swig.tab/ops compile-hiccup-impl id-gen id)))

(defmethod compile-hiccup-impl :swig.type/view
  [{id :db/id :as props} id-gen _]
  (cond-> props
    (:swig.view/ops props)
    (update :swig.view/ops compile-hiccup-impl id-gen id)))

(defmethod compile-hiccup-impl :swig.type/split
  [{id :db/id :as props} id-gen _]
  (cond-> props
    (:swig.split/ops props)
    (update :swig.split/ops compile-hiccup-impl id-gen id)))

(defmethod compile-hiccup-impl :swig.type/frame
  [{id :db/id :as props} id-gen _]
  (cond-> props
    (:swig.frame/ops props)
    (update :swig.frame/ops compile-hiccup-impl id-gen id)))

(defmethod compile-hiccup-impl :swig.type/operations
  [props id-gen parent]
  (let [id (swap! id-gen dec)]
    (cond-> (assoc props
                   :swig.ref/parent parent
                   :db/id id)
      (:swig.operations/ops props)
      (update :swig.operations/ops
              (fn [ops]
                (map #(assoc % :swig.ref/parent id) ops))))))

(defn hiccup->facts
  ([hiccup]
   (hiccup->facts nil hiccup))
  ([parent hiccup]
   (let [id-gen (atom -1)
         valid? true #_(m/validate :swig.spec/view hiccup {:registry spec/registry})]
     (if (not valid?)
       (throw (ex-info "Schema Validation Failed"
                       (m/explain :swig.spec/view hiccup {:registry spec/registry})))
       (let [facts ((fn run [parent idx [swig-type props children]]
                      (lazy-seq
                       (let [id (or (:db/id props) (swap! id-gen dec))]
                         (conj (vec (mapcat (partial run id)(range) children))
                               (compile-hiccup-impl
                                (cond-> (assoc props
                                               :db/id id
                                               :swig/index idx
                                               :swig/type swig-type)
                                  parent (assoc :swig.ref/parent parent))
                                id-gen
                                parent)))))
                    parent 0 hiccup)]
         facts)))))

(defn facts->hiccup
  ([facts]
   (facts->hiccup (d/entity facts root-view) facts))
  ([parent facts]
   (let [parent-id (:db/id parent)
         children  (d/q '[:find [(pull ?id [*]) ...]
                          :in $ ?parent
                          :where
                          [?id :swig.ref/parent ?parent]]
                        facts
                        parent-id)]
     [(:swig/type parent) (into {} (remove (comp internal-keys key)) parent)
      (mapv #(facts->hiccup % facts)
            (sort-by :swig/index children))])))

(comment

  (def tree
    [layout
     {:swig.split/orientation   :vertical
      :swig.split/split-percent 50}
     [:swig.type/view {:swig.view/active-tab 20
                       :db/id 10}
      [:swig.type/tab {:swig.tab/label "A"}]]])

  )
