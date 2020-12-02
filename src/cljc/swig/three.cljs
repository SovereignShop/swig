(ns swig.three)

(def schema
  [{:db/ident :swig.three.box/dims :db/valueType :db.type/tuple :db/cardinality :db.cardinality/one}
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
   {:db/ident :swig.three.object/rotation :db/valueType :db.type/tuple :db/cardinality :db.cardinality/one}])

(def registry
  {::perspective-camera
   [:tuple
    [:= :swig.type/three.perspective-camera]
    [:map
     [:swig.three.perspective-camera/fov :double]
     [:swig.three.perspective-camera/aspect :double]
     [:swig.three.perspective-camera/near :double]
     [:swig.three.perspective-camera/far :double]
     [:swig.three.perspective-camera/active :boolean]]]

   ::orthograhic-camera
   [:tuple
    [:= :swig.type/three.orthographic-camera]
    [:map
     [:swig.three.orthographic-camera/left :double]
     [:swig.three.orthographic-camera/right :double]
     [:swig.three.orthographic-camera/top :double]
     [:swig.three.orthographic-camera/bottom :double]
     [:swig.three.orthographic-camera/near :double]
     [:swig.three.orthographic-camera/far :double]
     [:swig.three.orthographic-camera/active :boolean]]]

   ::plane
   [:tuple
    [:= :swig.type/three.plane]
    [:map
     [:swig.three.plane/width :double]
     [:swig.three.plane/height :double]
     [:swig.three.plane/width-segments :integer]
     [:swig.three.plane/height-segments :integer]
     [:swig.three.plane/depth-segments :integer]
     [:swig.three.plane/material ::material]]]


   ::sphere
   [:tuple
    [:= :swig.type/three.sphere]
    [:map]]

   ::cylinder
   [:tuple
    [:= :swig.type/three.cylinder]
    [:map]]

   ::circle
   [:tuple
    [:= :swig.type/three.circle]
    [:map]]

   ::object
   [:tuple
    [:= :swig.type/three.object]
    [:map]]

   ::group
   [:tuple
    [:= :swig.type/three.group]
    [:map]]})

(defn perspective-camera [props & children]
  [:swig.type/three.perspective-camera props (vec children)])

(defn orthographic-camera [props & children]
  [:swig.type/three.orthographic-camera props (vec children)])

(defn plane [props & children]
  [:swig.type/three.plane props (vec children)])

(defn sphere [props & children]
  [:swig.type/three.sphere props (vec children)])

(defn cylinder [props & children]
  [:swig.type/three.cylinder props (vec children)])

(defn circle [props & children]
  [:swig.type/three.circle props (vec children)])

(defn object [props & children]
  [:swig.type/three.object props (vec children)])

(defn group [props & children]
  [:swig.type/three.group props (vec children)])
