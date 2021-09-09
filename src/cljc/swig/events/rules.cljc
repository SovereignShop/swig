(ns swig.events.rules)

(def get-descendants
  '[[(get-descendants ?parent ?child)
     [?parent :swig.ref/child ?child]]
    [(get-descendants ?parent ?child)
     [?parent :swig.ref/child ?p]
     (get-descendants ?p ?child)]])

(def get-ancestors
  '[[(get-ancestors ?gp ?p)
     [?gp :swig.ref/child ?p]]
    [(get-ancestors ?gp ?p)
     [?ggp :swig.ref/child ?gp]
     (get-ancestors ?ggp ?p)]])
