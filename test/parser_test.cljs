(ns parser-test
  (:require
   [datascript.core :as d]
   [cljs.test    :as t :refer-macros [is are deftest testing]]
   [swig.parser :refer [hiccup->facts facts->hiccup]]))

(deftest test-hiccup<->facts
  (let [tree
        [:swig.type/split
         {:swig.split/orientation :vertical
          :swig.split/split-percent 50}
         [:swig.type/view {:swig.view/active-tab 20}]]
        [:swig.type/view {:swig.view/active-tab 10}
         [:swig.type/tab {:swig.tab/name "Test"}]]
        [:swig.type/view {:swig.view/active-tab 30}]]
    (is (= tree
           (facts->hiccup 1
                          (d/db-with
                           (d/empty-db {:swig.ref/parent {:db/valueType :db.type/ref}})
                           (hiccup->facts tree)))))))
