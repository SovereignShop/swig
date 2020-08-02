(ns swig.core
  (:require
   [datascript.core :as d]
   [swig.views :as views]
   [swig.events :as e]
   #?@(:cljs [[swig.parser :refer [hiccup->facts]]
              [re-posh.core :as re-posh]
              [reagent.dom :as reagent]])))

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
   {:db/ident :swig.operations/ops :db/valueType :db.type/ref :db/cardinality :db.cardinality/many}])

(defn cell [props]
  [:swig.type/cell props])

(defn tab [props & children]
  [:swig.type/tab props children])

(defn view [props & children]
  [:swig.type/view props children])

(defn split [props & children]
  [:swig.type/split props children])

(defn window [props]
  [:swig.type/window props nil])

#?(:cljs
   (defn init [layout]
     (re-posh/dispatch-sync [::e/initialize (hiccup->facts layout)])))

#?(:cljs
   (defn render [view-id]
     (reagent/render [views/root-component view-id]
                     (.getElementById js/document "app"))))
