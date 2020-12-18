(ns swig.dispatch)

(defn swig-dispatch-fn
  ([props]
   (:swig/type props)))

(defmulti dispatch #'swig-dispatch-fn)

(defmethod dispatch :default
  ([props]
   [:div (str "No method found for props:" (into {} props))]))

(defmulti wrap (fn
                 ([props] (:swig/ident props))
                 ([props _] (:swig/ident props))))

(defmethod wrap :default
  ([props] [:div (str ::erorr " " props)])
  ([_ elem] elem))

(defmulti capability-handler (comp second list))
