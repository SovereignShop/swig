(ns tab-events-test
  (:require
   #?(:cljs [cljs.test :as t :refer-macros [is are deftest testing]]
      :clj [clojure.test :as tst :refer [is are deftest testing]])
   [test-utils :refer [to-ds-schema
                       test-tree
                       eav-added
                       query-tabs
                       query-views
                       query-parent
                       query-active-tab]]
   [datascript.core :as d]
   [malli.core :as m]
   [swig.spec :as s]
   [swig.core :as swig]
   [swig.parser :refer [hiccup->facts facts->hiccup]]
   [swig.events
    :as e
    :refer [set-active-tab
            enter-fullscreen
            exit-fullscreen
            move-tab
            kill-tab
            divide-tab
            join-views]]
   [clojure.set :as set]))


(deftest test-set-active-tab
  ;; Test set
  (let [db (d/db-with
            (d/empty-db (to-ds-schema swig/full-schema))
            (hiccup->facts test-tree))]
    (doseq [tab-id (query-tabs db)]
      (let [{parent-id :db/id active-tab :swig.view/active-tab}
            (query-parent db tab-id)
            {:keys [db-before db-after]}
            (d/with db (set-active-tab db parent-id tab-id))]
        (is (= (query-active-tab db-after parent-id) tab-id))
        (is (= (query-active-tab db-before parent-id) (:db/id active-tab)))))))

(deftest test-tabs-enter-exit-fullscreen
  (let [db (d/db-with
            (d/empty-db (to-ds-schema swig/full-schema))
            (hiccup->facts test-tree))]
    (doseq [tab-id (query-tabs db)]
      (let [db (d/db-with db [{:db/id (-> db (d/entity tab-id) :swig.ref/parent :db/id)
                               :swig.view/active-tab tab-id}])
            {enter-before :db-before enter-after :db-after}
            (d/with db (enter-fullscreen db  (d/entity db tab-id)))
            {exit-before :db-before exit-after :db-after}
            (d/with enter-after (exit-fullscreen enter-after (d/entity enter-after tab-id)))]
        (is (= enter-after exit-before))
        (is (= (mapv eav-added (d/datoms enter-before :eavt))
               (mapv eav-added (d/datoms exit-after :eavt))))))))

(deftest test-tabs-move
  ;; Test moving/unmoving each tab to each view.
  (let [db (d/db-with (d/empty-db (to-ds-schema swig/full-schema)) (hiccup->facts test-tree))]
    (doseq [tab-id (d/q '[:find [?id ...]  :in $ :where [?id :swig/type :swig.type/tab]] db)]
      (let [parent-id (-> db (d/entity tab-id) :swig.ref/parent :db/id)]
        (doseq [view-id (query-views db)]
          (let [{before-move :db-before tx-data :tx-data}
                (d/with db (move-tab tab-id view-id))
                {after-return :db-after}
                (d/with db (move-tab tab-id parent-id))]
            (is (empty? (clojure.set/difference
                         (into #{} (map eav-added) (d/datoms before-move :eavt))
                         (into #{} (map eav-added) (d/datoms after-return :eavt)))))
            (is (if (= parent-id view-id) ;; Moving to current parent should produce no tx-data
                  (= tx-data [])
                  (= (mapv eav-added tx-data)
                     [[tab-id :swig.ref/parent parent-id false]
                      [tab-id :swig.ref/parent view-id true]])))))))))

(deftest test-tabs-divide-join
  (let [db
        (d/db-with
         (d/empty-db (to-ds-schema swig/full-schema))
         (hiccup->facts test-tree))]
    (doseq [tab-id (query-tabs db)]
      (let  [{divide-before :db-before divide-after :db-after tx-data :tx-data}
             (d/with db (divide-tab db tab-id :vertical))
             split-id (first (sequence (comp (filter #(= (:v %) :swig.type/split)) (map :e))
                                       tx-data))
             {join-after :db-after}
             (d/with db (join-views divide-after (d/entity divide-after split-id)))]
        (is (pos? split-id))
        (is (empty? (clojure.set/difference
                     (into #{} (map eav-added) divide-before)
                     (into #{} (map eav-added) join-after))))
        (is (empty? (clojure.set/difference
                     (into #{} (map eav-added) join-after)
                     (into #{} (map eav-added) divide-before))))))))
