(ns swig.spec
  (:require [clojure.spec.alpha :as s]))

(s/def :swig.view/ops keyword?)
(s/def :swig.tab/ops keyword?)
(s/def :swig.tab/fullscreen boolean?)
(s/def :swig.ref/parent int?)

(s/def :swig.type/view
  (s/keys :req [:swig.view/active-tab
                :swig.view/ops]))

(s/def :swig.type/tab
  (s/keys :req [:swig.tab/fullscreen
                :swig.tab/ops
                :swig.tab/label]))

(s/def :swig.type/split
  (s/keys :req [:swig.split/ops
                :swig.split/orientation
                :swig.split/split-percent]))
