(ns tab-events-test
  (:require
   #?(:cljs [cljs.test :as t :refer-macros [is deftest]]
      :clj [clojure.test :as tst :refer [is deftest]])
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
   [swig.events.tab :as e]
   [swig.events.view :as ve]
   [swig.events.utils :as eu]
   [clojure.pprint :as pprint]
   [clojure.set :as set]))

(deftest test-set-active-tab
  ;; Test set
  (let [db (d/db-with
            (d/empty-db (to-ds-schema swig/full-schema))
            (parser/hiccup->facts test-tree))]
    (doseq [tab-id (query-tabs db)]
      (let [{parent-id :db/id active-tab :swig.view/active-tab}
            (query-parent db tab-id)
            {:keys [db-before db-after]}
            (d/with db (e/set-active-tab db parent-id tab-id))]
        (is (= (query-active-tab db-after parent-id) tab-id))
        (is (= (query-active-tab db-before parent-id) (:db/id active-tab)))))))


(deftest test-tabs-enter-exit-fullscreen
  (let [db (d/db-with
            (d/empty-db (to-ds-schema swig/full-schema))
            (parser/hiccup->facts test-tree))]
    (doseq [tab-id (take 1 (query-tabs db))]
      (let [db (d/db-with db [{:db/id (-> db (d/entity tab-id) :swig.ref/parent :db/id)
                               :swig.view/active-tab tab-id}])
            {enter-before :db-before enter-after :db-after}
            (d/with db (e/enter-fullscreen db tab-id))
            {exit-before :db-before exit-after :db-after}
            (d/with enter-after (e/exit-fullscreen enter-after tab-id))]
        (is (= (mapv eav-added (d/datoms enter-before :eavt))
               (mapv eav-added (d/datoms exit-after :eavt))))))))

(deftest test-tabs-move
  ;; Test moving/unmoving each tab to each view.
  (let [db (d/db-with (d/empty-db (to-ds-schema swig/full-schema)) (parser/hiccup->facts test-tree))]
    (doseq [tab-id (d/q '[:find [?id ...]  :in $ :where [?id :swig/type :swig.type/tab]] db)]
      (let [parent-id (:db/id (eu/get-parent (d/entity db tab-id)))]
        (doseq [view-id (query-views db)]
          (let [{before-move :db-before after-move :db-after tx-data :tx-data}
                (d/with db (e/move-tab db tab-id view-id))
                {after-return :db-after}
                (d/with after-move (e/move-tab after-move tab-id parent-id))]
            (is (empty? (clojure.set/difference
                         (into #{} (map eav-added) (d/datoms before-move :eavt))
                         (into #{} (map eav-added) (d/datoms after-return :eavt)))))
            (if (= parent-id view-id) ;; Moving to current parent should produce no tx-data
              (is (= tx-data []))
              (is (= (mapv eav-added tx-data)
                     [[parent-id :swig.ref/child tab-id false]
                      [view-id :swig.ref/child tab-id  true]])))))))))

(deftest test-tabs-divide-join
  (let [db
        (d/db-with
         (d/empty-db (to-ds-schema swig/full-schema))
         (parser/hiccup->facts test-tree))]
    (doseq [tab-id (query-tabs db)]
      (let  [{divide-before :db-before divide-after :db-after tx-data :tx-data}
             (d/with db (e/divide-tab db tab-id :vertical))
             split-id (first (sequence (comp (filter #(= (:v %) :swig.type/split)) (map :e))
                                       tx-data))
             {join-after :db-after}
             (d/with db (ve/join-views divide-after split-id))]
        (is (pos? split-id))
        (is (empty? (clojure.set/difference
                     (into #{} (map eav-added) divide-before)
                     (into #{} (map eav-added) join-after))))
        (is (empty? (clojure.set/difference
                     (into #{} (map eav-added) join-after)
                     (into #{} (map eav-added) divide-before))))))))

#_(deftest test-tabs-duplicate-and-divide
  (let [db
        (d/db-with
         (d/empty-db (to-ds-schema swig/full-schema))
         (parser/hiccup->facts test-tree))]
    (doseq [tab-id (query-tabs db)]
      (let [children '...
            duplicate-tab (tab '...)]
        ))))
