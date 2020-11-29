(ns test-drag
  (:require
   #?(:clj [clojure.test :as t]
      :cljs [cljs.test :as t :include-macros true])
   [datascript.core :as d]
   [swig.parser :refer [hiccup->facts]]
   [swig.events :as e]
   [swig.core :as swig]
   [test-utils :as tu]))

(def test-tree
  [:swig.type/view
   {:swig/ident :swig/root-view,
    :swig.view/active-tab [:swig/ident :tabs/a]}
   [[:swig.type/view
     {:swig/ident :views/a,
      :swig.container/capabilities #{:swig.capability/drag},
      :swig.view/active-tab [:swig/ident :tabs/a]}
     [[:swig.type/frame
       {:swig/ident :skyhook.constants.ui-idents/test-frame,
        :swig.frame/width 400.0,
        :swig.frame/height 200.0,
        :swig.frame/top 200.0,
        :swig.frame/left 200.0}
       [[:swig.type/view
         {:swig/ident :idents/a,
          :swig.view/active-tab
          [:swig/ident :tabs/d]}
         [[:swig.type/tab
           {:swig/ident :tabs/e,
            :swig.tab/label
            {:swig/type :swig.type/cell, :swig.cell/element "E"}}
           []]
          [:swig.type/tab
           {:swig/ident :tabs/d,
            :swig.tab/label
            {:swig/type :swig.type/cell, :swig.cell/element "D"}}
           []]]]]]
      [:swig.type/tab
       {:swig/ident :tabs/f,
        :swig.tab/label
        {:swig/type :swig.type/cell, :swig.cell/element "F"}}
       []]
      [:swig.type/tab
       {:swig/ident :tabs/c,
        :swig.tab/label
        {:swig/type :swig.type/cell, :swig.cell/element "C"}}
       []]
      [:swig.type/tab
       {:swig/ident :tabs/b,
        :swig.tab/label
        {:swig/type :swig.type/cell, :swig.cell/element "B"}}
       []]
      [:swig.type/tab
       {:swig/ident :tabs/a,
        :swig.tab/label
        {:swig/type :swig.type/cell, :swig.cell/element "A"}}
       []]]]]])

(t/deftest test-container-drag
  (let [db (-> swig/full-schema
               tu/to-ds-schema
               d/empty-db
               (d/db-with (hiccup->facts test-tree)))]
    (doseq [frame-id (tu/query-frames db)]
      (let [frame (d/entity db frame-id)
            {drag-start-tx-data :tx-data after-drag-start-db :db-after}
            (d/with db (e/drag-start db frame-id 100 5))
            {drag-frame-tx-data :tx-data after-drag-db :db-after}
            (d/with after-drag-start-db (e/drag-frame after-drag-start-db frame-id 300 300))
            {drag-stop-tx-data :tx-data}
            (d/with after-drag-db (e/drag-stop after-drag-db frame-id))
            container-id (-> frame :swig.ref/parent :db/id)]
        (t/are [x y] (= x y)
          (into #{} (map tu/eav-added) drag-start-tx-data)
          #{[frame-id :swig.frame/offset-left 100 true]
            [frame-id :swig.frame/offset-top 5 true]
            [container-id :swig.capability.drag/frame-id frame-id true]}

          (into #{} (comp (filter (comp true? :added)) (map tu/eav-added)) drag-frame-tx-data)
          #{[frame-id :swig.frame/left 300 true]
            [frame-id :swig.frame/top 300 true]}

          (into #{} (map tu/eav-added) drag-stop-tx-data)
          #{[container-id :swig.capability.drag/frame-id frame-id false]})))))
