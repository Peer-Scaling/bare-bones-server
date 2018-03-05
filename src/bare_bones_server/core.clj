(ns bare-bones-server.core
  (:require [clojure.tools.nrepl.server
             :refer (start-server stop-server)]))

(def server (atom nil))

(def default-port 7888)

(defn start
  [& port]
  (let [s (start-server :port (or port default-port))]
    (reset! server s)))

(defn stop
  [server]
  (stop-server server))