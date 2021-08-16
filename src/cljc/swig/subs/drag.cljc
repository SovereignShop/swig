(ns swig.subs.drag
  (:require
   [swig.macros :refer-macros [def-sub]]))

(def-sub :swig.subs.drag/drag-frame-id
  [:find ?id .
   :in $ ?container-id
   :where
   [?container-id :swig.capability.drag/frame-id ?id]])

(def-sub :swig.subs.drag/get-drag-frame
  [:find ?frame-id .
   :in $ ?view-id
   :where
   [?frame-id :swig.ref/child ?view-id]
   [?frame-parent-id :swig.ref/child ?frame-id]
   [?frame-parent-id :swig.container/capabilities :swig.capability/drag]])

(def-sub :swig.subs.drag/drag-offsets
  [:find [?left ?top]
   :in $ ?frame-id
   :where
   [?frame-id :swig.frame/offset-left ?left]
   [?frame-id :swig.frame/offset-top ?top]])
