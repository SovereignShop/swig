(ns swig.events.split
  (:require
   [swig.macros :as m]
   [datascript.core :as d]))

#?(:cljs
   (defn update-view-sizes [db]
     (let [view-ids (d/q '[:find [?view ...]
                           :in $
                           :where
                           [?view :swig/type :swig.type/view]]
                         db)]
       (for [view-id view-ids
             :let [view-element (.getElementById js/document (str "swig-" view-id))]
             :when view-element]
         {:db/id view-id
          :swig.view/top (.-offsetTop view-element)
          :swig.view/left (.-offsetLeft view-element)
          :swig.view/width (.-offsetWidth view-element)
          :swig.view/height (.-offsetHeight view-element)}))))

(m/def-event-ds :swig.events.split/set-split-percent
  [db split-id percent]
  (into
   [[:db/add split-id :swig.split/split-percent (+ (float percent) 0.001)]]
   #?(:cljs (update-view-sizes db) :clj [])))
