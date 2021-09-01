(ns swig.core
  #?(:cljs
     (:require
      [swig.events.core]
      [swig.views :as views]
      [swig.parser :refer [hiccup->facts]]
      [swig.dispatch]
      [re-posh.core :as re-posh]
      [reagent.dom :as reagent])))

(def full-schema
  [{:db/ident :swig.dispatch/handler :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig.ref/previous-parent :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.ref/parent :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.ref/child :db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   {:db/ident :swig.operation/name :db/valueType :db.type/keyword :db/cardinality :db.cardinality/many}
   {:db/ident :swig/ident :db/unique :db.unique/identity :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig/type :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig/index :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.tab/fullscreen :db/valueType :db.type/boolean :db/cardinality :db.cardinality/one}
   {:db/ident :swig.tab/handler :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig.tab/label :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.element/ops :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.tab/order :db/valueType :db.type/long :db/cardinality :db.cardinality/one}
   {:db/ident :swig.tab/previous-view-id :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.view/active-tab :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.view/previous-active-tab :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :swig.view/tab-type :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig.view/left :db/valueType :db.type/number :db/cardinality :db.cardinality/one}
   {:db/ident :swig.view/top :db/valueType :db.type/number :db/cardinality :db.cardinality/one}
   {:db/ident :swig.view/width :db/valueType :db.type/number :db/cardinality :db.cardinality/one}
   {:db/ident :swig.view/height :db/valueType :db.type/number :db/cardinality :db.cardinality/one}
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
   {:db/ident :swig/has-focus? :db/valueType :db.type/boolean :db/cardinality :db.cardinality/one}
   {:db/ident :swig/event :db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   {:db/ident :swig.event/name :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :swig.event/on :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}])

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
     (re-posh/dispatch-sync [:swig.events.core/initialize (hiccup->facts layout)])))

#?(:cljs
   (defn render [view-id]
     (reagent/render [views/root-component view-id]
                     (.getElementById js/document "app"))))
