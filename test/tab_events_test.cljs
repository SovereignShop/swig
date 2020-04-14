(ns tab-events-test
  (:require
   [cljs.test    :as t :refer-macros [is are deftest testing]]
   [datascript.core :as d]
   [cljs.reader :refer [read-string]]
   [swig.parser :refer [hiccup->facts facts->hiccup]]
   [swig.core :refer [root-view]]
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

(def test-tree
  [:swig.type/view
   {:swig.view/active-tab 20}
   [:swig.view/tab
    {:swig.tab/name "A"
     :db/id 20}
    [:swig.type/split
     {:swig.split/orientation   :vertical
      :swig.split/split-percent 50}
     [:swig.type/view {:swig.view/active-tab 20}]
     [:swig.type/view {:swig.view/active-tab 10
                       :db/id              9}
      [:swig.type/tab {:swig.tab/name "B"
                       :db/id       10}]
      [:swig.type/tab {:swig.tab/name "C"
                       :db/id       11}]]
     [:swig.type/view {:swig.view/active-tab 30 :db/id 12}]]]])

(deftest test-set-active-tab
  (let [db
        (d/db-with
         (d/empty-db {:swig.ref/parent      {:db/valueType :db.type/ref}
                      :swig.view/active-tab {:db/valuetype :db.tpye/ref}})
         (hiccup->facts test-tree))]
    (is (= (facts->hiccup (:db-after (d/with db (set-active-tab db [::e/set-active-tab 9 11]))))
           (assoc-in test-tree [2 2 3 1 :swig.view/active-tab] 11)))))

(deftest test-enter-fullscreen
  (let [db
        (d/db-with
         (d/empty-db {:swig.ref/parent      {:db/valueType :db.type/ref}
                      :swig.view/active-tab {:db/valuetype :db.type/ref}})
         (hiccup->facts test-tree))
        fullscreen-tx-data (enter-fullscreen db [nil 10])
        tree (facts->hiccup (d/db-with db fullscreen-tx-data))]
    (is (= (read-string (prn-str tree))
           [:swig.type/view
            #:swig.view{:active-tab #:db{:id 10}, :previous-active-tab 20}
            [:swig.type/tab
             {:db/id 10,
              :swig.ref/previous-parent 9,
              :swig.tab/name "B",
              :swig.tab/fullscreen true}]
            [:swig.view/tab
             {:db/id 20, :swig.tab/name "A"}
             [:swig.type/split
              #:swig.split{:orientation :vertical, :split-percent 50}
              [:swig.type/view #:swig.view{:active-tab #:db{:id 20}}]
              [:swig.type/view
               {:db/id 9, :swig.view/active-tab #:db{:id 10}}
               [:swig.type/tab {:db/id 11, :swig.tab/name "C"}]]
              [:swig.type/view
               {:db/id 12, :swig.view/active-tab #:db{:id 30}}]]]]))))

(deftest test-exit-fullscreen
  (let [db
        (d/db-with
         (d/empty-db {:swig.ref/parent      {:db/valueType :db.type/ref}
                      :swig.view/active-tab {:db/valuetype :db.type/ref}})
         (hiccup->facts test-tree))
        enter-fullscreen-tx-data (exit-fullscreen db [nil 10])
        db (d/db-with db (enter-fullscreen db [nil 10]))
        db (d/db-with db (exit-fullscreen db [nil 10]))
        tree (facts->hiccup db)]
    (= (read-string (prn-str tree))
       [:swig.type/view
        #:swig.view{:active-tab #:db{:id 20}, :previous-active-tab 20}
        [:swig.view/tab
         {:db/id 20, :swig.tab/name "A"}
         [:swig.type/split
          #:swig.split{:orientation :vertical, :split-percent 50}
          [:swig.type/view #:swig.view{:active-tab #:db{:id 20}}]
          [:swig.type/view
           {:db/id 9, :swig.view/active-tab #:db{:id 10}}
           [:swig.type/tab
            {:swig.tab/name "B",
             :swig.ref/previous-parent 9,
             :swig.tab/fullscreen false,
             :db/id 10,
             :swig.tab/fullscreen true}]
           [:swig.type/tab {:db/id 11, :swig.tab/name "C"}]]
          [:swig.type/view #:swig.view{:active-tab #:db{:id 30}}]]]])))

(deftest test-move-tab
  (let [db
        (d/db-with
         (d/empty-db {:swig.ref/parent      {:db/valueType :db.type/ref}
                      :swig.view/active-tab {:db/valuetype :db.type/ref}})
         (hiccup->facts test-tree))
        db (d/db-with db (move-tab db [nil 10 12]))
        tree (facts->hiccup db)]
    (is (= (read-string (prn-str tree))
           [:swig.type/view
            #:swig.view{:active-tab #:db{:id 20}}
            [:swig.view/tab
             {:db/id 20, :swig.tab/name "A"}
             [:swig.type/split
              #:swig.split{:orientation :vertical, :split-percent 50}
              [:swig.type/view #:swig.view{:active-tab #:db{:id 20}}]
              [:swig.type/view
               {:db/id 9, :swig.view/active-tab #:db{:id 10}}
               [:swig.type/tab {:db/id 11, :swig.tab/name "C"}]]
              [:swig.type/view
               {:db/id 12, :swig.view/active-tab #:db{:id 30}}
               [:swig.type/tab {:db/id 10, :swig.tab/name "B"}]]]]]))))

(deftest test-kill-tab)

(deftest test-divide-tab
  (let [db
        (d/db-with
         (d/empty-db {:swig.ref/parent      {:db/valueType :db.type/ref}
                      :swig.view/active-tab {:db/valuetype :db.type/ref}})
         (hiccup->facts
          [:swig.type/view
           {:swig.view/active-tab 20}
           [:swig.view/tab
            {:swig.tab/name "A"
             :db/id 20}
            [:swig.type/split
             {:swig.split/orientation   :vertical
              :swig.split/split-percent 50}
             [:swig.type/view {:swig.view/active-tab 20}]
             [:swig.type/view {:swig.view/active-tab 10
                               :db/id              9}
              [:swig.type/tab {:swig.tab/name "B"
                               :db/id       10}]
              [:swig.type/tab {:swig.tab/name "C"
                               :db/id       11}]]]]]))
        db (d/db-with db (divide-tab db [nil 10 :vertical]))
        tree (facts->hiccup db)]
    (is (= (read-string (prn-str tree))
           [:swig.type/view
            #:swig.view{:active-tab #:db{:id 20}}
            [:swig.view/tab
             {:db/id 20, :swig.tab/name "A"}
             [:swig.type/split
              #:swig.split{:orientation :vertical, :split-percent 50}
              [:swig.type/split
               #:swig.split{:ops #{:swig.ops.splits/re-orient
                                   :swig.ops.splits/join},
                            :orientation :vertical,
                            :split-percent 50}
               [:swig.type/view
                #:swig.view{:active-tab #:db{:id 10},
                            :ops #{:swig.ops.tabs/divide-vertical
                                   :swig.ops.tabs/divide-horizontal}}
                [:swig.type/tab {:db/id 11,
                                 :swig.tab/name "C"}]]
               [:swig.type/view
                {:db/id 9,
                 :swig.view/active-tab #:db{:id 10}}
                [:swig.type/tab {:db/id 10, :swig.tab/name "B"}]]]
              [:swig.type/view #:swig.view{:active-tab #:db{:id 20}}]]]]))))


(deftest test-join-views
  (let [db
        (d/db-with
         (d/empty-db {:swig.ref/parent      {:db/valueType :db.type/ref}
                      :swig.view/active-tab {:db/valuetype :db.type/ref}})
         (hiccup->facts
          [:swig.type/view
           {:swig.view/active-tab 20}
           [:swig.view/tab
            {:swig.tab/name "A"
             :db/id       20}
            [:swig.type/split
             {:swig.split/orientation   :vertical
              :swig.split/split-percent 50
              :db/id                  14}
             [:swig.type/view {:swig.view/active-tab 20}]
             [:swig.type/view {:swig.view/active-tab 10
                               :db/id              9}
              [:swig.type/tab {:swig.tab/name "B"
                               :db/id       10}]
              [:swig.type/tab {:swig.tab/name "C"
                               :db/id       11}]]]]]))
        tree (facts->hiccup (d/db-with db (join-views db [nil 14])))]
    (is (= (read-string (prn-str tree))
           [:swig.type/view #:swig.view{:active-tab #:db{:id 20}}
            [:swig.view/tab {:db/id 20, :swig.tab/name "A"}
             [:swig.type/view #:swig.view{:active-tab #:db{:id 20}}
              [:swig.type/tab {:db/id 10, :swig.tab/name "B"}]
              [:swig.type/tab {:db/id 11, :swig.tab/name "C"}]]]]))))
