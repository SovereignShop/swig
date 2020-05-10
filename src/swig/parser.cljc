(ns swig.parser
  (:require
   #?(:cljs [cljs.spec.alpha :as s]
      :clj [clojure.spec.alpha :as s])
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

(def internal-keys #{:db/id :swig/index :swig.ref/parent :swig/type})

(defmulti compile-hiccup-impl (fn [parent elem] (:swig/type elem)))

(defmethod compile-hiccup-impl :default
  [_ props]
  props)

(defmethod compile-hiccup-impl :swig.type/tab
  [_ {id :db/id :as props}]
  (cond-> props
    (:swig.tab/ops props)
    (update :swig.tab/ops
            (fn [ops]
              (map #(assoc % :swig.ref/parent id) ops)))))

(defmethod compile-hiccup-impl :swig.type/view
  [_ {id :db/id :as props}]
  (cond-> props
    (:swig.view/ops props)
    (update :swig.view/ops
            (fn [ops]
              (map #(assoc % :swig.ref/parent id) ops)))))

(defmethod compile-hiccup-impl :swig.type/split
  [_ {id :db/id :as props}]
  (cond-> props
    (:swig.split/ops props)
    (update :swig.split/ops
            (fn [ops]
              (map #(assoc % :swig.ref/parent id) ops)))))

(defn hiccup->facts
  ([hiccup]
   (hiccup->facts -100000 hiccup))
  ([parent hiccup]
   (let [id        (volatile! -1)
         conformed (s/conform ::node hiccup)]
     ((fn run [parent idx hiccup]
        (lazy-seq
         (when (seq hiccup)
           (match hiccup
                  [(:or :element :empty-element) props]
                  (let [id (or (-> props :args :db/id) (vreset! id (dec @id)))
                        args (:args props)]
                    (conj (vec (mapcat (partial run id)
                                       (range)
                                       (match (:body props)
                                              [:nodes nodes] nodes
                                              [:nodes-list nodes] nodes)))
                          (compile-hiccup-impl
                           parent
                           (cond-> (assoc args
                                          :swig.ref/parent parent
                                          :db/id id
                                          :swig/index idx
                                          :swig/type (:name props))
                             (not= parent -100000)
                             (assoc :swig.ref/parent parent)))))
                  :cljs.spec.alpha/invalid
                  (s/explain ::node hiccup)))))
      parent 0 conformed))))

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
     (into [(:swig/type parent) (into {} (remove (comp internal-keys key)) parent)]
           (mapv #(facts->hiccup % facts)
                 (sort-by :swig/index children))))))

(comment

  (def tree
    [layout
     {:swig.split/orientation   :vertical
      :swig.split/split-percent 50}
     [:swig.type/view {:swig.view/active-tab 20
                       :db/id 10}
      [:swig.type/tab {:swig.tab/label "A"}]]])

  )
