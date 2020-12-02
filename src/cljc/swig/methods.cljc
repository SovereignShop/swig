(ns swig.methods)

(defn swig-dispatch-fn
  ([props]
   (:swig/type props)))

(defmulti dispatch #'swig-dispatch-fn)

(defmethod dispatch :default
  ([props]
   [:div (str "No method found for props:" props)]))

(defmulti wrap (fn
                 ([props] (:swig/ident props))
                 ([props _] (:swig/ident props))))

(defmethod wrap :default [_ elem] elem)

(defmulti capability-handler (comp second list))
