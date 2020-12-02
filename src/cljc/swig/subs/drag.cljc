(ns swig.subs.drag
  (:require
   [swig.macros :as m]))

(m/def-sub :swig.subs.drag/drag-frame-id
  '[:find ?id .
    :in $ ?container-id
    :where
    [?container-id :swig.capability.drag/frame-id ?id]])

(m/def-sub :swig.subs.drag/get-drag-frame
  '[:find ?frame-id .
    :in $ ?view-id
    :where
    [?view-id :swig.ref/parent ?frame-id]
    [?frame-id :swig.ref/parent ?frame-parent-id]
    [?frame-parent-id :swig.container/capabilities :swig.capability/drag]])

(m/def-sub :swig.subs.drag/drag-offsets
  '[:find [?left ?top]
    :in $ ?frame-id
    :where
    [?frame-id :swig.frame/offset-left ?left]
    [?frame-id :swig.frame/offset-top ?top]])
