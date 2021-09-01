(ns view-events-test
  (:require
   #?(:clj [clojure.test :as t :refer [deftest is]]
      :cljs [cljs.test :as t :include-macros true :refer-macros [is deftest]])
   [test-utils :refer [to-ds-schema
                       root-view
                       test-tree
                       eav-added
                       query-tabs
                       query-views
                       query-parent
                       split-tree
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
            :when (eu/get-parent (d/entity db view-id))]
      (let [{before-duplicate :db-before after-duplicate :db-after}
            (d/with db (ve/divide-view db view-id :vertical))
            view (d/entity after-duplicate view-id)
            parent (eu/get-parent view)
            views (:swig.ref/child parent)]
        (is (= (count views) 2))
        (is (apply = (map :swig.ref/child views)))
        (is (= (:swig/type parent) :swig.type/split))))))

(deftest test-goto-left
  (let [db (d/db-with (d/empty-db (to-ds-schema swig/full-schema))
                      (parser/hiccup->facts split-tree))]
    (doseq [view-id (query-views db)
            :when (= (:swig/type (eu/get-parent (d/entity db view-id)))
                     :swig.type/split)]
      (let [{before-goto-left :db-before after-goto-left :db-after}
            (d/with db (ve/goto-left db view-id))
            before-view (d/entity before-goto-left view-id)
            after-view (d/entity after-goto-left view-id)]
        (when (pos? (:swig/index before-view))
          (is (and (not (:swig/has-focus? after-view))
                   (:swig/has-focus? before-view))))))))

(deftest test-query-descendants
  (let [db (d/db-with (d/empty-db (to-ds-schema swig/full-schema))
                      (parser/hiccup->facts split-tree))]
    (is (= #{[2 :swig.type/view] [3 :swig.type/split] [1 :swig.type/view]}
           (d/q '[:find ?c ?type
                  :in $ ?p %
                  :where
                  (get-descendants ?p ?c)
                  [?c :swig/type ?type]]
                db
                root-view
                ve/get-descendants)))))
