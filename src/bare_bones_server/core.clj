(ns bare-bones-server.core
  (:require [clojure.tools.nrepl.server :refer [default-handler start-server stop-server]]
            [cemerick.piggieback :refer [wrap-cljs-repl]]
            [bare-bones-server.team-repl :refer [wrap-classpath]]))

(defonce server (atom nil))

(def default-port 7888)

(defn start
  [cljs-eval? & port]
  (let [p (or port default-port)
        s (if cljs-eval?
            (do
              (println "Starting CLJS env on port" p)
              (start-server :port p :handler (default-handler #'wrap-cljs-repl)))
            (do
              (println "Starting CLJ env on port" p)
              (start-server :port p :handler (default-handler #'wrap-classpath))))]
    (reset! server s)))

(defn stop
  [server]
  (stop-server server))