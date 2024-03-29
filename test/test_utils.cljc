(ns test-utils
  (:require  #?(:clj [clojure.test :as t]
                :cljs [cljs.test :as t :include-macros true])
             [datascript.core :as d]))

(def root-view [:swig/ident :swig/root-view])

(defn to-ds-schema [schema]
  (into {}
        (map (fn [m]
               [(:db/ident m)(cond-> m
                               true                                  (dissoc m :db/ident)
                               (not= (:db/valueType m) :db.type/ref) (dissoc m :db/valueType))]))
        schema))

(def simple-tree
  [:swig.type/view
   {:swig/ident :swig/root-view
    :swig.view/active-tab [:swig/ident :tabs/a]}
   [[:swig.type/tab
     {:swig.tab/label
      {:swig/type :swig.type/cell
       :swig.cell/element "A"}}]]])

(def test-tree
  [:swig.type/view
   {:swig/ident           :swig/root-view}
   [[:swig.type/view
     {:swig/ident           :views/root,
      :swig.view/active-tab [:swig/ident :tabs/a]}
     [[:swig.type/tab
       {:swig/ident :tabs/a,
        :swig.tab/label
        {:swig/type :swig.type/cell, :swig.cell/element "A"}}
       [[:swig.type/frame
         {:swig/ident        :frames/a,
          :swig.frame/width  200.0,
          :swig.frame/height 200.0,
          :swig.frame/top    200.0,
          :swig.frame/left   200.0}
         []]]]
      [:swig.type/tab
       {:swig/ident :tabs/b,
        :swig.tab/label
        {:swig/type :swig.type/cell,
         :swig.cell/element "B"}}
       []]
      [:swig.type/tab
       {:swig/ident     :tabs/c,
        :swig.tab/label {:swig/type :swig.type/cell,
                         :swig.cell/element "C"}}
       []]
      [:swig.type/tab
       {:swig/ident     :tabs/d,
        :swig.tab/label {:swig/type :swig.type/cell,
                         :swig.cell/element "D"}}
       []]]]]])

(def split-tree
  [:swig.type/view
   {:swig/ident           :swig/root-view}
   [[:swig.type/split
     {:swig.split/orientation :vertical
      :swig.split/split-percent 50.1}
     [[:swig.type/view
       {:swig/ident :views/a
        :swig/has-focus? true}
       []]
      [:swig.type/view
       {:swig/ident :views/b}
       []]]]]])

(defn query-tabs [db]
  (d/q '[:find [?id ...] :in $ :where [?id :swig/type :swig.type/tab]] db))

(defn query-views [db]
  (d/q '[:find [?id ...] :in $ :where [?id :swig/type :swig.type/view]] db))

(defn query-frames [db]
  (d/q '[:find [?id ...] :in $ :where [?id :swig/type :swig.type/frame]] db))

(defn query-parent [db id]
  (->> (d/q '[:find ?parent . :in $ ?child :where [?parent :swig.ref/child ?child]] db id)
       (d/entity db)))

(defn query-active-tab [db view-id]
  (d/q '[:find ?active-tab . :in $ ?view-id :where [?view-id :swig.view/active-tab ?active-tab]]
       db
       view-id))

(def eav-added (juxt :e :a :v :added))
