(ns swig.events.drag
  (:require
   [swig.events.utils :as event-utils]
   [datascript.core :as d]
   [swig.macros :as m]))

(m/def-event-ds ::drag-start
  [db frame-id left top]
  (when-let [drag-container (first (filter (comp :swig.capability/drag
                                                 set
                                                 :swig.container/capabilities)
                                           (event-utils/ancestor-seq (d/entity db frame-id))))]
    [{:db/id (:db/id drag-container)
      :swig.capability.drag/frame-id frame-id}
     {:db/id frame-id
      :swig.frame/offset-left left
      :swig.frame/offset-top top}]))

(m/def-event-ds ::drag-stop
  [db frame-id]
  (when-let [drag-container (first (filter (comp :swig.capability/drag
                                                 set
                                                 :swig.container/capabilities)
                                           (event-utils/ancestor-seq (d/entity db frame-id))))]
    [[:db.fn/retractAttribute (:db/id drag-container) :swig.capability.drag/frame-id]]))

(m/def-event-ds ::drag-frame
  [db frame-id left top]
  [{:db/id frame-id
    :swig.frame/left left
    :swig.frame/top top}])
