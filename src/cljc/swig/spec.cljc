(ns swig.spec
  (:require
   [malli.core :as m]))

(def swig-registry
  {::label
   [:map
    [:swig/type {:optional false} [:= :swig.type/cell]]
    [:swig.cell/element {:optional false} string?]]

   ::ident
   [:or pos-int? [:tuple keyword? keyword?]]

   ::operation
   [:map
    [:swig.operation/name {:optional false} keyword?]]

   ::frame
   [:tuple
    [:= :swig.type/frame]
    [:map
     [:swig.frame/width {:optional false} :double]
     [:swig.frame/height {:optional false} :double]
     [:swig.frame/left {:optional false} :double]
     [:swig.frame/top {:optional false} :double]]
    [:vector [:or [:ref ::view] [:ref ::tab] [:ref ::split]]]]

   ::tab
   [:tuple
    [:= :swig.type/tab]
    [:map
     [:swig.tab/fullscreen {:optional true} :boolean]
     [:swig.element/ops {:optional true} any?]]
    [:vector [:or [:ref ::frame] [:ref ::view] [:ref ::split]]]]

   ::view-ops
   [:vector [:map]]

   ::view
   [:tuple
    [:= :swig.type/view]
    [:map
     [:swig.view/active-tab {:optional true} ::ident]
     [:swig.view/previous-active-tab {:optional true} ::ident]
     [:swig.element/ops {:optional true} ::view-ops]]
    [:vector [:or [:ref ::window] [:ref ::frame] [:ref ::tab] [:ref ::view] [:ref ::split]]]]

   ::split-ops
   [:vector [:map]]

   ::split
   [:tuple
    [:= :swig.type/split]
    [:map
     [:swig.element/ops {:optional true} [:ref ::split-ops]]
     [:swig.split/orientation {:optional false} [:or [:= :horizontal] [:= :vertical]]]
     [:swig.split/split-percent {:optional false} :int]]
    [:vector [:or [:ref ::view] [:ref ::tab] [:ref ::split]]]]

   ::window
   [:tuple
    [:= :swig.type/window]
    [:map
     [:swig.window/child {:optional true} any?]]
    [:vector any?]]})


(def registry
  (merge (m/class-schemas)
         (m/comparator-schemas)
         (m/base-schemas)
         (m/predicate-schemas)
         (m/type-schemas)
         swig-registry))
