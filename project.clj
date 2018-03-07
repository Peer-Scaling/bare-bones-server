(defproject bare-bones-server "0.0.1-PLAYGROUND"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [com.cemerick/piggieback "0.2.2"]
                 [org.clojure/clojurescript "1.9.946"]]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]})
