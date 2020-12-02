#?(:cljs
   (ns swig.views
     (:require
      [re-com.core :as re]
      [re-posh.core :as re-posh]
      [swig.methods :as methods]
      [swig.views.tab]
      [swig.views.view]
      [swig.views.split]
      [swig.views.frame]
      [swig.views.operations]
      [swig.views.window]
      [swig.subs :as subs]))
   :clj
   (ns swig.views
     (:require
      [swig.subs :as subs])))

(def root-view [:swig/ident :swig/root-view])

#?(:cljs
   (defn root-component [view-id]
     (let [elem @(re-posh/subscribe [::subs/get-element view-id])]
       [re/v-box
        :height "100vh"
        :width "100vw"
        :children
        [[methods/dispatch elem]]]))
   :clj
   (defn root-component [view-id]
     (throw (Exception. "Not implemented for clj."))))
