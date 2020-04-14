(ns deploy
  (:require
   [badigeon.compile :as compile]
   [badigeon.prompt :as prompt]
   [badigeon.sign :as sign]
   [badigeon.deploy :as deploy]
   [badigeon.jar :as jar]
   [clojure.tools.deps.alpha.reader :as deps-reader]))

(defn deploy-lib []
  (let [deps (:deps (deps-reader/slurp-deps "deps.edn"))]
    (jar/jar 'org.cartesiantheatrics/swig {:mvn/version "0.0.1-SNAPSHOT"}
             {:out-path                "target/swig-0.0.1-SNAPSHOT.jar"
              :paths                   ["src"]
              :mvn/repos               '{"clojars" {:url "https://repo.clojars.org/"}}
              :exclusion-predicate     jar/default-exclusion-predicate
              :allow-all-dependencies? true})
    (let [artifacts (-> [{:file-path "target/swig-0.0.1-SNAPSHOT.jar"
                          :extension "jar"}
                         {:file-path "pom.xml"
                          :extension "pom"}]
                        (badigeon.sign/sign {:command "gpg"}))
          password  (badigeon.prompt/prompt-password "Password: ")]
      (badigeon.deploy/deploy 'org.cartesiantheatrics/swig
                              "0.0.1-SNAPSHOT"
                              artifacts
                              {:id  "clojars"
                               :url "https://repo.clojars.org/"}
                              {:credentials     {:username "cartesiantheatrics"
                                                 :password password
                                                 ;;:private-key "/path/to/private-key"
                                                 ;;:passphrase  "passphrase"
                                                 }
                               :allow-unsigned? true}))))

(defn -main []
  (deploy-lib))

