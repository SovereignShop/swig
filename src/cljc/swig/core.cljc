(ns swig.core
  #?(:cljs
     (:require
      [swig.views :as views]
      [swig.parser :refer [hiccup->facts]]
      [re-posh.core :as re-posh]
      [reagent.dom :as reagent])))

(def full-schema
  [{:db/ident :swig.dispatch/handler :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig.ref/previous-parent :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.ref/parent :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.operation/name :db/valueType :db.type/keyword :db/cardinality :db.cardinality/many}
   {:db/ident :swig/ident :db/unique :db.unique/identity :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig/type :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig/index :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.tab/fullscreen :db/valueType :db.type/boolean :db/cardinality :db.cardinality/one}
   {:db/ident :swig.tab/handler :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig.tab/label :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.tab/ops :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.view/ops :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.split/ops :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.tab/order :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.tab/previous-view-id :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.view/active-tab :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.view/previous-active-tab :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.view/tab-type :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig.split/orientation :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig.split/split-percent :db/valueType :db.type/number :db/cardinality :db.cardinality/one}
   {:db/ident :swig.cell/element :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :swig.operations/ops :db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   {:db/ident :swig.frame/height :db/valueType :db.type/number :db/cardinality :db.cardinality/one}
   {:db/ident :swig.frame/width :db/valueType :db.type/number :db/cardinality :db.cardinality/one}
   {:db/ident :swig.frame/top :db/valueType :db.type/number :db/cardinality :db.cardinality/one}
   {:db/ident :swig.frame/left :db/valueType :db.type/number :db/cardinality :db.cardinality/one}
   {:db/ident :swig.container/capabilities :db/valueType :db.type/keyword :db/cardinality :db.cardinality/many}
   {:db/ident :swig.capability.drag/frame-id :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.frame/ops :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}

   {:db/ident :swig.three.box/dims :db/valueType :db.type/tuple :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.box/material :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.perspective-camera/fov :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.perspective-camera/aspect :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.perspective-camera/near :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.perspective-camera/far :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.perspective-camera/active :db/valueType :db.type/boolean :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.orthographic-camera/left :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.orthographic-camera/right :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.orthographic-camera/top :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.orthographic-camera/bottom :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.orthographic-camera/near :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.orthographic-camera/far :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.orthographic-camera/active :db/valueType :db.type/boolean :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.plane/width :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.plane/height :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.plane/width-segments :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.plane/height-segments :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.plane/depth-segments :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.plane/material :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.sphere/radius :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.sphere/width-segments :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.sphere/height-segments :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.sphere/phi-start :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.sphere/phi-length :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.sphere/theta-start :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.sphere/theta-length :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.sphere/material :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.cylinder/radius-top :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.cylinder/radius-bottom :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.cylinder/height :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.cylinder/radial-segments :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.cylinder/height-segments :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.cylinder/open-ended? :db/valueType :db.type/boolean :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.cylinder/theta-start :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.cylinder/theta-length :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.cylinder/material :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.circle/radius :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.circle/segments :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.cricle/theta-start :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.circle/theta-length :db/valueType :db.type/double :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.circle/material :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.material/color :db/valueType :db.type/tuple :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.object/position :db/valueType :db.type/tuple :db/cardinality :db.cardinality/one}
   {:db/ident :swig.three.object/rotation :db/valueType :db.type/tuple :db/cardinality :db.cardinality/one}

   ])

(defn cell [props]
  [:swig.type/cell props []])

(defn tab [props & children]
  [:swig.type/tab props (vec children)])

(defn view [props & children]
  [:swig.type/view props (vec children)])

(defn split [props & children]
  [:swig.type/split props (vec children)])

(defn window [props]
  [:swig.type/window props []])

(defn frame [props & children]
  [:swig.type/frame props (vec children)])

#?(:cljs
   (defn init [layout]
     (re-posh/dispatch-sync [:swig.events/initialize (hiccup->facts layout)])))

#?(:cljs
   (defn render [view-id]
     (reagent/render [views/root-component view-id]
                     (.getElementById js/document "app"))))
