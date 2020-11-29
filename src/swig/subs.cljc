(ns swig.subs
  (:require
   [swig.macros :refer-macros [def-sub def-pull-sub]]))

;; Generic ops

#?(:cljs
   (def-sub ::get-handler
     [:find ?handler .
      :in $ ?view-id
      :where
      [?view-id :swig.dispatch/handler ?handler]]))

#?(:cljs
   (def-sub ::get-type
     [:find ?type .
      :in $ ?id
      :where
      [?id :swig/type ?type]]))

#?(:cljs
   (def-sub ::get-children
     [:find (pull ?child-id [:db/id
                             :swig.tab/ops
                             :swig.container/capabilities
                             :swig/ident
                             :swig/index
                             :swig.dispatch/handler
                             :swig/type])
      :in $ ?id [?type ...]
      :where
      [?child-id :swig.ref/parent ?id]
      [?child-id :swig/type ?type]]))

#?(:cljs
   (def-pull-sub ::get-element
     [:db/id
      :swig/ident
      :swig/index
      :swig.dispatch/handler
      :swig.container/capabilities
      :swig/type]))

#?(:cljs
   (def-sub ::get-window-element
     [:find ?elem .
      :in $ ?win-id
      :where
      [?win-id :swig.window/child ?elem]]))

;; Tab ops
#?(:cljs
   (def-sub ::get-tabs
     [:find (pull ?tab-id [:swig.tab/fullscreen
                           :swig.tab/handler
                           :swig.ref/parent
                           {:swig.tab/label
                            [:swig/type
                             :swig.cell/element]}
                           :swig.tab/order
                           {:swig.tab/ops
                            [:swig/type
                             {:swig.operations/ops
                              [:swig/type]}]}
                           {:swig.container/capabilities []}
                           :swig/ident
                           :db/id
                           :swig.dispatch/handler
                           :swig/type])
      :in $ ?view-id
      :where
      [?tab-id :swig.ref/parent ?view-id]
      [?tab-id :swig/type :swig.type/tab]]))

#?(:cljs
   (def-pull-sub ::get-frame
     [:swig.frame/left
      :swig.frame/top
      :swig.frame/width
      :swig.frame/height]))

#?(:cljs
   (def-pull-sub ::get-tab
     [:swig.tab/fullscreen
      :swig.tab/handler
      :swig.tab/label
      :swig.tab/order
      :swig.tab/ops
      :swig/type
      :swig.ref/parent
      :swig/ident
      :db/id]))

;; View subs

#?(:cljs
   (def-sub ::get-active-tab
     [:find (pull ?tab-id [:swig.tab/fullscreen
                           :swig.tab/handler
                           :swig.tab/label
                           :swig.tab/order
                           :swig.tab/ops
                           :swig/type
                           :swig.ref/parent
                           :swig/ident
                           :db/id]) .
      :in $ ?view-id
      :where
      [?view-id :swig.view/active-tab ?tab-id]]))

#?(:cljs
   (def-pull-sub ::get-view-ops
     [:swig.view/ops
      :swig.view/tab-type]))

#?(:cljs
   (def-sub ::get-view-ids
     [:find [?view-id ...]
      :in $ ?split-id
      :where
      [?view-id :swig.ref/parent ?split-id]]))

;; Split subs

#?(:cljs
   (def-pull-sub ::get-split
     [:swig.split/split-percent
      :swig.split/ops
      :swig.split/orientation]))
;; Cells

#?(:cljs
   (def-pull-sub ::get-cell
     [:swig.cell/element
      :swig.dispatch/handler
      :swig/type
      :db/id]))

;; Operations

#?(:cljs
   (def-sub ::get-op-names
     [:find [?name ...]
      :in $ [?id ...]
      :where
      [?id :swig.operation/name ?name]]))

#?(:cljs
   (def-pull-sub ::get-operation
     [:db/id
      :swig/type]))

#?(:cljs
   (def-pull-sub ::get-operations
     [:db/id
      :swig/type
      :swig.operations/ops]))


;; Drag subs

#?(:cljs
   (def-sub ::drag-frame-id
     [:find ?id .
      :in $ ?container-id
      :where
      [?container-id :swig.capability.drag/frame-id ?id]]))

#?(:cljs
   (def-sub ::get-drag-frame
     [:find ?frame-id .
      :in $ ?view-id
      :where
      [?view-id :swig.ref/parent ?frame-id]
      [?frame-id :swig.ref/parent ?frame-parent-id]
      [?frame-parent-id :swig.container/capabilities :swig.capability/drag]]))

#?(:cljs
   (def-sub ::drag-offsets
     [:find [?left ?top]
      :in $ ?frame-id
      :where
      [?frame-id :swig.frame/offset-left ?left]
      [?frame-id :swig.frame/offset-top ?top]]))
