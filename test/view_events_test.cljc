(ns view-events-test
  (:require
   #?(:clj [clojure.test :as t :refer [deftest is]]
      :cljs [cljs.test :as t :include-macros true :refer-macros [is deftest]])
   [test-utils :refer [to-ds-schema
                       test-tree
                       eav-added
                       query-tabs
                       query-views
                       query-parent
                       query-active-tab]]
   [datascript.core :as d]
   [swig.core :as swig]
   [swig.parser :as parser]
   [swig.events.view :as ve]
   [swig.events.utils :as eu]
   [clojure.pprint :as pprint]
   [clojure.set :as set]))


(deftest test-duplicate-view
  (let [db (d/db-with (d/empty-db (to-ds-schema swig/full-schema))
                      (parser/hiccup->facts test-tree))]
    (doseq [view-id (query-views db)
            :when (not= (->> view-id (d/entity db) :swig/ident) :swig/root-view)]
      (let [{before-duplicate :db-before after-duplicate :db-after}
            (d/with db (ve/duplicate-view db view-id))
            view (d/entity after-duplicate view-id)
            parent (eu/get-parent view)
            views (:swig.ref/child parent)]
        (is (= (count views) 2))
        (is (apply = (map :swig.ref/child views)))
        (is (= (:swig/type parent) :swig.type/split))))))
