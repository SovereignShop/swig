(ns swig.events.resize
  (:require
   [swig.events.utils :as event-utils]
   [datascript.core :as d]
   [swig.macros :as m]))

(m/def-event-ds ::resize-start
  [db frame-id left top]
  (when-let [resize-container (first (filter (comp :swig.capability/resize
                                                   set
                                                   :swig.container/capabilities)
                                             (event-utils/ancestor-seq (d/entity db frame-id))))]
    (let [frame (d/entity db frame-id)]
      [{:db/id                           (:db/id resize-container)
        :swig.capability.resize/frame-id frame-id}
       {:db/id                             frame-id
        :swig.capability.resize/start-left (- (:swig.frame/width frame) left)
        :swig.capability.resize/start-top  (- (:swig.frame/height frame) top)}])))

(m/def-event-ds ::resize-frame
  [db frame-id left top]
  [{:db/id frame-id
    :swig.frame/width left
    :swig.frame/height top}])

(m/def-event-ds ::resize-stop
  [db frame-id]
  (when-let [resize-container (first (filter (comp :swig.capability/resize
                                                   set
                                                   :swig.container/capabilities)
                                             (event-utils/ancestor-seq (d/entity db frame-id))))]
    [[:db.fn/retractAttribute (:db/id resize-container) :swig.capability.resize/frame-id]]))
