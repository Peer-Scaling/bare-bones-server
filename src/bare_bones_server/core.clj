(ns bare-bones-server.core
  (:require [clojure.tools.nrepl.server
             :refer (start-server stop-server) :as server]
            [bare-bones-server.team-repl :refer [wrap-classpath]]))

(defonce server (atom nil))

(def default-port 7888)

(defn start [& port]
  (reset! server (start-server :port (or port default-port)
                               :handler (server/default-handler #'wrap-classpath))))

(defn stop [server]
  (stop-server server))