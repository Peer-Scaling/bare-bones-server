(ns bare-bones-server.team-repl
  (:require [clojure.tools.nrepl.transport :as transport]
            [clojure.tools.nrepl.middleware :as middleware]
            [clojure.tools.nrepl.middleware.session :as session]))

(defn wrap-classpath [handler]
  (fn [{:keys [id op transport] :as request}]
    (if (= op "classpath")
      (transport/send
        transport
        {:id        id
         :classpath (->> (java.lang.ClassLoader/getSystemClassLoader)
                         .getURLs
                         (map str))})
      (handler request))))

(middleware/set-descriptor!
  #'wrap-classpath
  {:requires #{#'session/session}
   :expects  #{}
   :handles  {"classpath"
              {:doc      "Return the Java CLASSPATH"
               :requires {}
               :optional {}
               :returns  {"classpath" "The CLASSPATH"}}}})
