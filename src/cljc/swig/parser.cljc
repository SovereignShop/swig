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

(def internal-keys #{:swig/index :swig/type :swig.ref/child})

(defmulti compile-hiccup-impl (fn [elem _ _] (:swig/type elem)))

(defmethod compile-hiccup-impl :default
  [props _ _]
  props)

(defmethod compile-hiccup-impl :swig.type/tab
  [{id :db/id :as props} id-gen _]
  (cond-> props
    (:swig.element/ops props)
    (update :swig.element/ops compile-hiccup-impl id-gen id)))

(defmethod compile-hiccup-impl :swig.type/view
  [{id :db/id :as props} id-gen _]
  (cond-> props
    (:swig.element/ops props)
    (update :swig.element/ops compile-hiccup-impl id-gen id)))

(defmethod compile-hiccup-impl :swig.type/split
  [{id :db/id :as props} id-gen _]
  (cond-> props
    (:swig.element/ops props)
    (update :swig.element/ops compile-hiccup-impl id-gen id)))

(defmethod compile-hiccup-impl :swig.type/frame
  [{id :db/id :as props} id-gen _]
  (cond-> props
    (:swig.frame/ops props)
    (update :swig.frame/ops compile-hiccup-impl id-gen id)))

(defmethod compile-hiccup-impl :swig.type/operations
  [props id-gen parent]
  (let [id (swap! id-gen dec)]
    (assoc props :db/id id)))

(defn hiccup->facts
  ([hiccup]
   (hiccup->facts nil hiccup))
  ([parent hiccup]
   (let [id-gen (atom -1)
         valid? true #_ (m/validate :swig.spec/view hiccup {:registry spec/registry})]
     (if (not valid?)
       (throw (ex-info "Schema Validation Failed"
                       (m/explain :swig.spec/view hiccup {:registry spec/registry})))
       (let [facts ((fn run [parent idx [swig-type props children]]
                      (let [id (or (:db/id props) (swap! id-gen dec))
                            c (mapv (partial run id) (range) children)]
                        (cond-> (conj (vec (apply concat c))
                                      (compile-hiccup-impl
                                       (cond-> (assoc props
                                                      :db/id id
                                                      :swig/index idx
                                                      :swig.ref/child (mapv (comp :db/id peek) c)
                                                      :swig/type swig-type)
                                         (meta props) (assoc :swig/meta (meta props)))
                                       id-gen
                                       parent))
                          (:object/form props) (conj [:db/add (:object/form props) :three/object id]))))
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
                          [?parent :swig.ref/child ?id]]
                        facts
                        parent-id)]
     [(:swig/type parent) (into (with-meta {} (:swig/meta parent))
                                (remove (comp internal-keys key))
                                parent)
      (mapv #(facts->hiccup % facts)
            (sort-by :swig/index children))])))


(defn substitute-idents [db e]
  (if (and (map? e)
           (= (count e) 1)
           (:db/id e))
    [:swig/ident (d/q '[:find ?ident . :in $ ?id :where [?id :swig/ident ?ident]] db (:db/id e))]
    e))

(defn facts->hiccup-debug
  ([facts]
   (facts->hiccup-debug (d/entity facts root-view) facts))
  ([parent facts]
   (let [parent-id (:db/id parent)
         children  (d/q '[:find [(pull ?id [*]) ...]
                          :in $ ?parent
                          :where
                          [?parent :swig.ref/child ?id]]
                        facts
                        parent-id)]
     [(:swig/type parent) (into (with-meta {} (:swig/meta parent))
                                (comp
                                 (remove (comp internal-keys key))
                                 (map (fn [[k v]]
                                        [k (substitute-idents facts v)])))
                                parent)
      (mapv #(facts->hiccup-debug % facts)
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
