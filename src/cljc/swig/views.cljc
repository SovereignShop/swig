#?(:cljs
   (ns swig.views
     (:require
      [re-com.core :as re]
      [re-posh.core :as re-posh]
      [swig.dispatch :as methods]
      [swig.views.tab]
      [swig.views.view]
      [swig.views.split]
      [swig.views.frame]
      [swig.views.operations]
      [swig.views.window]
      [swig.subs.drag]
      [swig.subs.element]
      [swig.subs.frame]
      [swig.subs.operations]
      [swig.subs.resize]
      [swig.subs.split]
      [swig.subs.tab]
      [swig.subs.view]
      [swig.events.core]
      [swig.events.drag]
      [swig.events.resize]
      [swig.events.split]
      [swig.events.tab]
      [swig.events.view]))
   :clj
   (ns swig.views))

(def root-view [:swig/ident :swig/root-view])

#?(:cljs
   (defn root-component [view-id]
     (let [elem @(re-posh/subscribe [:swig.subs.element/get-element view-id])]
       [re/v-box
        :height "100vh"
        :width "100vw"
        :children
        [[methods/dispatch elem]]]))
   :clj
   (defn root-component [_]
     (throw (Exception. "Not implemented for clj."))))
